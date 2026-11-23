# Kafka en Pedidos 360: contrato de eventos y convenciones

Fuente de verdad para productores, consumidores, el administrador y la infraestructura.
RabbitMQ NO se modifica: Kafka se suma para reporteria y auditoria (casos de uso
"Panel de operaciones en tiempo real" y "Auditoria y trazabilidad inalterable" del curso).

## 1. Cluster (docker-compose.kafka.yml, estilo de la guia 3.2.1 ampliado a 3+3)

| Servicio | Imagen | Red interna (microservicios) | Host EC2 |
|---|---|---|---|
| zookeeper-1..3 | confluentinc/cp-zookeeper:7.3.2 | zookeeper-N:2181 | no se publica |
| kafka-1 | confluentinc/cp-kafka:7.3.2 | kafka-1:29092 | localhost:9092 |
| kafka-2 | confluentinc/cp-kafka:7.3.2 | kafka-2:29093 | localhost:9093 |
| kafka-3 | confluentinc/cp-kafka:7.3.2 | kafka-3:29094 | localhost:9094 |
| kafka-ui | provectuslabs/kafka-ui (version fija) | - | 8080 |

Bootstrap para todos los microservicios: `KAFKA_BOOTSTRAP_SERVERS=kafka-1:29092,kafka-2:29093,kafka-3:29094`
(en `application.yml`: `spring.kafka.bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`).

## 2. Topicos (los dos que pide la pauta: 3 particiones y 3 replicas)

| Topico | Productor | Clave | cleanup.policy | retention.ms | min.insync.replicas | Por que |
|---|---|---|---|---|---|---|
| `pedidos-eventos` | ms-pedidos | `pedidoId` | `delete` | 604800000 (7 dias) | 2 | Historial de negocio: se relee para reconstruir metricas y auditar la ultima semana |
| `inventario-movimientos` | ms-productos | `productoId` | `compact` | (no aplica) | 2 | Interesa el ultimo stock de cada producto: compactar conserva el ultimo evento por clave |

Topicos de error (Dead Letter Topic, mismo numero de particiones porque el DLT publica en la misma particion):
`pedidos-eventos.DLT` y `inventario-movimientos.DLT`: 3 particiones, 3 replicas, `cleanup.policy=delete`, `retention.ms=1209600000` (14 dias).

Quien declara cada topico (bean `NewTopic` con `TopicBuilder`, idempotente):
- ms-pedidos declara `pedidos-eventos`; ms-productos declara `inventario-movimientos`.
- ms-reportes y ms-auditoria declaran los dos `.DLT` (declararlos dos veces no causa error).
Valores (particiones, replicas, retention, cleanup, min.insync) en `application.yml`, bloque `kafka.topicos`; nunca escritos en el codigo.

## 3. Eventos (JSON, records Java validados con jakarta.validation)

La clave Kafka va como String. El productor envia sin headers de tipo (`spring.json.add.type.headers: false`)
porque cada servicio tiene su propia clase; cada consumidor deserializa a su propio record con los mismos campos.
Header `correlation-id` = `eventId` (para el MDC del consumidor).

### 3.1 `pedidos-eventos` -> `PedidoEvento`
```json
{
  "eventId": "6f0c1d2e-...-uuid",
  "tipo": "PEDIDO_CREADO",
  "fecha": "2026-11-17T22:30:00Z",
  "pedidoId": 9,
  "localId": "local-01",
  "clienteId": "sub-del-jwt",
  "estado": "RECIBIDO",
  "estadoAnterior": null,
  "modalidadEntrega": "RETIRO_EN_TIENDA",
  "total": 3000.00,
  "items": [ { "productoId": 1, "nombreProducto": "Pan amasado", "cantidad": 2, "precioUnitario": 1500.00 } ],
  "usuario": "Joaquin Moya",
  "rol": "CLIENTE"
}
```
- `tipo`: `PEDIDO_CREADO` | `ESTADO_CAMBIADO`.
- En `ESTADO_CAMBIADO`, `estado` es el nuevo y `estadoAnterior` el previo; `items` y `total` van igual (el evento es autosuficiente: el consumidor no necesita llamar a otro servicio).
- `usuario` y `rol`: quien hizo la accion (claim `name`, si no `preferred_username`, si no `sub`; rol = primer valor del claim `roles`, si no `CLIENTE`). Lo pide el caso de uso de auditoria del curso.
- Obligatorios (`@NotNull`/`@NotBlank`): eventId, tipo, fecha, pedidoId, localId, estado, total, usuario, rol. `total` >= 0.

### 3.2 `inventario-movimientos` -> `MovimientoInventarioEvento`
```json
{
  "eventId": "uuid",
  "tipo": "STOCK_DESCONTADO",
  "fecha": "2026-11-17T22:30:00Z",
  "productoId": 1,
  "localId": "local-01",
  "sku": "local-01-PAN",
  "nombre": "Pan amasado",
  "cantidad": 2,
  "stockResultante": 75,
  "usuario": "Joaquin Moya",
  "rol": "CLIENTE"
}
```
- `tipo`: `PRODUCTO_CREADO` | `STOCK_DESCONTADO` | `STOCK_REPUESTO`.
- `cantidad`: unidades movidas (positivo). En `PRODUCTO_CREADO` es el stock inicial.
- `stockResultante`: stock despues del movimiento (por eso el topico puede compactarse por `productoId`).
- Obligatorios: eventId, tipo, fecha, productoId, localId, nombre, cantidad (>= 0), stockResultante (>= 0), usuario, rol.

## 4. Productores (ms-pedidos, ms-productos)
- Publican DESPUES del commit (igual que el publisher de RabbitMQ) y si Kafka falla solo lo registran: el pedido o el stock nunca fallan por Kafka.
- Conviven con RabbitMQ: no se cambia nada de la mensajeria existente.
- Configuracion del productor (en `application.yml`): `acks: all`, `enable.idempotence: true`, `retries`, `delivery.timeout.ms: 120000`,
  `max.in.flight.requests.per.connection: 5`, tuning del curso `batch.size: 32768`, `linger.ms: 20`, `compression.type: snappy`,
  `key-serializer: StringSerializer`, `value-serializer: org.springframework.kafka.support.serializer.JacksonJsonSerializer`.

## 5. Consumidores (dos microservicios, dos consumer groups)

| Servicio | group-id | Lee | Que hace |
|---|---|---|---|
| ms-reportes | `grupo-reporteria` | ambos topicos | Metricas en tiempo real en memoria; al arrancar relee desde el inicio (las metricas son idempotentes: se calculan desde el ultimo estado por pedidoId / productoId) |
| ms-auditoria | `grupo-auditoria` | ambos topicos | Guarda cada evento en MySQL (`ms_auditoria_db`), idempotente por `eventId` unico |

Mismo topico, grupos distintos = cada servicio recibe TODOS los eventos. Dentro de cada grupo `concurrency: 3`
= 3 consumidores que se reparten las 3 particiones (asignacion `RoundRobinAssignor`, como el ejemplo del curso).

Configuracion del consumidor: `auto-offset-reset: earliest`, `enable-auto-commit: false`, `ack-mode: RECORD`,
`max.poll.records: 50`, `session.timeout.ms: 30000`, `heartbeat.interval.ms: 10000`, `max.poll.interval.ms: 300000`,
`ErrorHandlingDeserializer` envolviendo `JacksonJsonDeserializer` con el tipo del record de cada topico.

Manejo de errores (material "Manejo de Errores en Arquitecturas de Streaming"):
- `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` -> `<topico>.DLT` en la misma particion.
- Backoff exponencial: 3 reintentos, 1 s inicial, x2, maximo 10 s.
- No reintentables (directo al DLT): deserializacion, validacion (`@Valid`) y `EventoInvalidoException`.
- Logging contextual con MDC `correlationId` (header `correlation-id`) + topico/particion/offset/clave.
- Idempotencia en auditoria: `eventId` UNIQUE; duplicado -> `log.warn` y se descarta.

## 6. Microservicios nuevos

| Servicio | Puerto host (contenedor 8080) | Rutas (todas con JWT de Azure) | Roles |
|---|---|---|---|
| ms-kafka-admin | 8084 | `/api/kafka/**` | ADMIN_GENERAL |
| ms-reportes | 8085 | `GET /api/reportes/metricas` | ADMIN_GENERAL, OPERADOR_COCINA |
| ms-auditoria | 8086 | `GET /api/auditoria/eventos` | ADMIN_GENERAL |

Seguridad: igual que ms-rabbit-admin (copiar `AzureAdProperties`, `AudienceValidator`,
`AzureRolesJwtAuthenticationConverter` y un `SecurityConfig` con las reglas de la tabla). Sin CORS (lo resuelve el API Gateway).
Paquete base: `cl.duocuc.pedidos360.<servicio>` (`kafkaadmin`, `reportes`, `auditoria`). Java 21, Spring Boot 4.1.1,
`spring-boot-starter-kafka`. Dockerfile igual al de los demas (`eclipse-temurin:21-jre`, copia `target/*.jar`).
