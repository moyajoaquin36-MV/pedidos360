# Pedidos 360 — contexto para Claude Code

Leer completo antes de hacer nada. Estado al 23-11-2026: las 3 partes estan en `main` (PR #5 mergeado).

## Quien y para que
- Proyecto de Joaquin Moya (Duoc UC, DSY1107 Desarrollo Cloud Native I). Sistema de pedidos para 20 panaderias/cafeterias.
- Se evalua por partes: EP1-2 (API Gateway + Azure), EP3-4 (RabbitMQ), **EP5 encargo + EP6 presentacion (Kafka) = la parte actual**.
- En la parte 1 saco 4,4 porque no supo explicar su codigo. Por eso: explicar en espanol de Chile, claro y sin relleno; guiarlo paso a paso en AWS/Azure (no domina la terminal); que entienda cada decision para defenderla oralmente.
- Pautas de EP5/EP6: PDFs en su carpeta Descargas (no estan en el repo). Resumen al final de este archivo.

## Arquitectura
Angular (MSAL) → Azure Entra ID (OIDC + PKCE) → AWS API Gateway (JWT) → EC2 con Docker Compose:

| Servicio | Puerto host | Que hace |
|---|---|---|
| ms-pedidos | 8081 | Pedidos/estados. Publica en RabbitMQ y en Kafka (`pedidos-eventos`) |
| ms-productos | 8082 | Catalogo/stock. Publica en RabbitMQ y en Kafka (`inventario-movimientos`) |
| ms-rabbit-admin | 8083 | Control plane RabbitMQ (`/api/rabbitmq`) |
| ms-kafka-admin | 8084 | Admin Kafka: topicos, configs, particiones, consumer groups, lag, reset offsets (`/api/kafka`) |
| ms-reportes | 8085 | Consumer `grupo-reporteria`: metricas en tiempo real (`GET /api/reportes/metricas`) |
| ms-auditoria | 8086 | Consumer `grupo-auditoria`: guarda cada evento en MySQL (`GET /api/auditoria/eventos`) |
| MySQL, rabbitmq-1/2, nginx (frontend :80) | — | `backend/docker-compose.aws.yml` |
| zookeeper-1..3, kafka-1..3, kafka-ui (:8080) | — | `backend/docker-compose.kafka.yml` |

Docs clave: `docs/kafka-contrato.md` (fuente de verdad de Kafka), `backend/README.md`, `docs/api-gateway.md`,
`docs/demo-rabbitmq.md`, `docs/postman/pedidos360.postman_collection.json`.

## Convenciones
- Un cambio = una rama + PR con descripcion (que cambia, por que, como se probo) + merge a `main` con merge commit.
  Commits como `-c user.name="moyajoaquin36-MV" -c user.email="moyajoaquin36@gmail.com"`.
- Java 21, **Spring Boot 4.1.1**, Jackson 3 (`tools.jackson`), spring-amqp 4.1, **spring-kafka 4.1** (usar
  `JacksonJsonSerializer/Deserializer`, no `JsonSerializer`), kafka-clients 4.2 (compatible con brokers cp-kafka 7.3.2).
- Comentarios en espanol sin tildes; nombres de colas/topicos/grupos siempre en `application.yml`, nunca a mano.
- Build y tests: JDK 21, desde `backend/`: `./mvnw package` (130 tests en verde al 19-11). Frontend: `npm ci && npx ng build --configuration production`.
- Cosas que ya fallaron (no repetir): Spring Boot 4 no crea `RabbitAdmin` solo (bean explicito); un `MessageConverter` como @Bean
  rompe el ACK manual de RabbitMQ; rutas `ANY /{proxy+}` en el Gateway capturan el preflight OPTIONS; un correo con `+` falla en el
  autorregistro de Azure.

## AWS (Learner Lab, us-east-1) y Azure
- EC2 `i-04e606b971be0fd3a` (t3.large desde el 22-11, por Kafka), IP elastica `174.129.161.62`,
  security group `sg-0782a8487bdcac91d`, Amazon Linux 2023, usuario `ec2-user`.
- API Gateway HTTP `ng3zai38m7` (`https://ng3zai38m7.execute-api.us-east-1.amazonaws.com`), autorizador JWT `k345fo`
  (issuer `https://login.microsoftonline.com/0079c3b5-6d24-4e9a-ae29-879747fa5377/v2.0`, audience `69bfb4d4-1adf-46e5-933a-9e1c2be64a40`,
  scope `access_as_user`). Cada ruta es explicita y apunta por HTTP_PROXY a `http://174.129.161.62:<puerto>/<ruta>`.
- Azure tenant `pedidos360dsy1107`; apps Pedidos360-Frontend (SPA, PKCE) y Pedidos360-API (roles CLIENTE, OPERADOR_COCINA, ADMIN_GENERAL);
  usuarios de prueba cliente@ y operador@; flujo de usuario `B2X_1_registropedidos360`.
- **Reglas que se respetaron siempre:**
  - Claude NUNCA escribe credenciales. Para renovar las de AWS, Joaquin copia el bloque de *AWS Details → AWS CLI* y ejecuta a mano
    `Get-Clipboard | Set-Content "$env:USERPROFILE\.aws\credentials" -Encoding ascii`. Caducan cada ~4 h.
  - Los cambios al security group los hace Joaquin en la consola (el modo auto bloquea abrir puertos). Guiarlo fila por fila.
  - Acceso a la EC2: EC2 Instance Connect con llave temporal (`ssh-keygen` en un temporal, `aws ec2-instance-connect send-ssh-public-key`
    vale 60 s, luego `ssh -i`). La regla SSH del SG permite solo la IP de Joaquin (cambia segun la red: ver https://checkip.amazonaws.com)
    y `18.206.107.24/29` (EC2 Instance Connect desde el navegador).
- En la EC2, `~/pedidos360/` (sin git): `docker-compose.aws.yml`, `.env` (secretos: DB, Azure, RabbitMQ; no tocar ni mostrar),
  `rabbitmq.conf`, `deploy/`, `mysql-init/`, `frontend-dist/` (lo sirve nginx) y `ms-*/Dockerfile` + `ms-*/target/*.jar`.
  Desplegar = compilar los jars en el PC, `scp` de jars/compose/archivos y `docker compose ... up -d --build`.
  Hay respaldos `~/respaldo-parte1-*`.

## Estado al 23-11-2026
Partes 1, 2 y 3 en `main` (parte 3 = PR #5) y desplegadas en la EC2 (t3.large, 17 contenedores). 130 tests en verde.
- Kafka verificado en la nube: topicos 3x3 con ISR completo, `.DLT` funcionando, lag (ms-auditoria detenido + offsets
  rebobinados → lag 5 → al volver descarta 5 duplicados por eventId → lag 0) y caida de `kafka-2` (era el controlador: pasa al 3,
  62/62 particiones online, ISR vuelve a 3 al encenderlo; despues se hizo `kafka-leader-election --election-type preferred`).
- Postman: carpetas "Kafka admin (ADMIN_GENERAL)" y "Kafka consumidores" (no se han corrido: necesitan token de Joaquin).
- Guion de demo: `docs/demo-kafka.md`.
- Fuera del repo (en `Documents` de Joaquin): `Presentacion-Pedidos360.pptx` (30 diapositivas: 1-16 partes 1-2, 17-30 Kafka;
  respaldo de la version EP4 en `Presentacion-Pedidos360-EP4.pptx`) y `prompt-estudio-pedidos360.md` (prompt para ChatGPT).
  Las capturas de Kafka UI se sacaron con Edge headless desde el PC (`--headless=new --screenshot`; Kafka UI solo acepta la IP de Joaquin).
- `Pan amasado` de `local-01` (id 31) tiene stock 0: el pedido 13 se llevo 72. Reponer antes de la demo.

## Pendiente
1. Joaquin: pegar la captura de `/panel` en la diapositiva 24 (recuadro punteado), reponer stock y correr en Postman las
   carpetas de Kafka con un token nuevo.
2. Estudiar con el prompt y ensayar con `docs/demo-kafka.md`.
3. Despues de la presentacion: cerrar en el SG las reglas 22 y 15672-15673 que quedaron en 0.0.0.0/0.

## Resumen de EP5 (codigo) y EP6 (presentacion)
- EP5: config completa del cliente Kafka 12 · productores/consumidores con los 2 topicos y DTO 12 · admin crear/listar/mantener 6 ·
  productores con DTO y serializers 14 · consumidores @KafkaListener con logica separada 14 · manejo de errores (validacion, logging,
  reintentos) 12 · parametros de topicos (retencion, cleanup, particiones, replicas) 12 · consumer groups 12 · admin de topicos y grupos 6.
  Ademas: docker-compose de Kafka con Zookeeper y brokers, y de Kafka UI.
- EP6: rol de 3 ZK + 3 brokers y tolerancia a fallos 10% · justificar 2 topicos de 3 particiones y 3 replicas 10% · Kafka UI 5% ·
  flujo productor→consumidor 12% · diseno de los eventos 6% · reaccion en tiempo real 12% · administracion (particiones, replicas,
  retention, consumer groups) 15% · metricas offsets/lag/distribucion 15% · casos de uso 15%. Mostrar tambien lo de partes 1 y 2.

## Estilo del profesor (material del curso, para replicarlo)
- Compose: `confluentinc/cp-zookeeper` y `cp-kafka` 7.3.2, `provectuslabs/kafka-ui`, heap 256M, listeners `PLAINTEXT://kafka-N:2909N` +
  `PLAINTEXT_HOST://localhost:909N`. Su ejemplo tenia 1 ZK + 2 brokers; la pauta pide 3 + 3.
- Crea los topicos en Kafka UI con 3 particiones (si se autocrean quedan con 1). Productor con clave = todos los pasos de una orden en la
  misma particion y en orden. Un `groupId` por servicio. Lag: Kafka UI → Consumers → columna Lag.
- Errores: `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` a `<topico>.DLT` en la misma particion, `FixedBackOff(2000,3)` o
  exponencial, `@Valid`, `ErrorHandlingDeserializer`, idempotencia con tabla, MDC `correlation-id`. Tuning: batch 32 KB, linger 20 ms, snappy.
- Sus 5 casos de uso para Pedidos 360: panel de operaciones en tiempo real (implementado: ms-reportes), auditoria inalterable con marca
  temporal y rol (implementado: ms-auditoria), control de inventario con alertas de quiebre, notificaciones con ventanas de tiempo,
  enrutamiento de comandas a cocina.
- Kafka vs RabbitMQ segun el material: en RabbitMQ el mensaje desaparece al consumirse; Kafka es un log inmutable que varios grupos leen a
  su ritmo y se puede reprocesar.
