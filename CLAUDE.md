# Pedidos 360 — contexto para Claude Code

Leer completo antes de hacer nada. Estado al 19-11-2026, rama de trabajo `parte3/kafka`.

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
- EC2 `i-04e606b971be0fd3a` (t3.medium, **hay que pasarla a t3.large** por Kafka), IP elastica `174.129.161.62`,
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

## Estado al 19-11-2026
Hecho (rama `parte3/kafka`, NO desplegado, nada probado contra un Kafka real porque el PC no tiene Docker):
contrato de eventos, compose Kafka 3 ZK + 3 brokers + Kafka UI, productores en ms-pedidos/ms-productos, ms-kafka-admin,
ms-reportes, ms-auditoria, panel en tiempo real `/panel` en el frontend, script `backend/deploy/kafka-topicos.sh`.
Partes 1 y 2 (Gateway, Azure, RabbitMQ) ya estan en `main` y desplegadas.

## Pendiente, en orden
1. **Desplegar** (pedir OK a Joaquin antes de cada cambio en AWS):
   - Renovar credenciales AWS (Joaquin). Pasar la EC2 a **t3.large**: stop → `modify-instance-attribute --instance-type t3.large` → start (la IP elastica se mantiene).
   - Joaquin abre en el SG: 8084-8086 desde 0.0.0.0/0 (el Gateway llega por IP publica, igual que 8081-8083) y 8080 (Kafka UI) solo para su IP.
   - Subir a `~/pedidos360/`: `docker-compose.aws.yml`, `docker-compose.kafka.yml`, `deploy/kafka-topicos.sh`, `mysql-init/`, los 6 jars + Dockerfiles de los modulos nuevos.
   - Crear `ms_auditoria_db` + GRANT a mano (el volumen MySQL ya existe; el comando esta comentado en `backend/mysql-init/01-crear-bases.sql`).
   - `docker compose -f docker-compose.aws.yml -f docker-compose.kafka.yml --env-file .env up -d --build`; verificar con `deploy/kafka-topicos.sh describir`
     que `pedidos-eventos` e `inventario-movimientos` quedaron con 3 particiones y 3 replicas (los crean los beans `NewTopic`; si no, `crear`).
   - Gateway: rutas con JWT + scope para `/api/kafka/...` (10 endpoints de ms-kafka-admin), `GET /api/reportes/metricas` y `GET /api/auditoria/eventos`.
   - Frontend: `npx ng build --configuration production` y copiar `frontend/dist/pedidos360-frontend/browser/*` a `~/pedidos360/frontend-dist/`.
2. **Probar de punta a punta** y corregir: crear un pedido → aparece en el panel y en auditoria; Kafka UI (topicos 3x3, consumer groups, lag);
   detener ms-auditoria, crear pedidos, ver crecer el lag y bajar al arrancarlo; mensaje invalido → `.DLT`.
3. Postman: carpetas para ms-kafka-admin, reportes y auditoria (mismo estilo que la carpeta "RabbitMQ admin").
4. Push + PR de `parte3/kafka` a `main` y merge.
5. Guiar a Joaquin en las capturas que pide EP6 (ver abajo).
6. PPT: agregar las diapositivas de Kafka a `Presentacion-Pedidos360.pptx` (Joaquin la tiene en su PC; NO va al repo porque es publico
   y tiene correos). Mismo estilo: 16:9 13.333x7.5 in, Calibri, fondo blanco, colores `2B1A16` (espresso), `D9772B` (naranjo), `F2B632` (amarillo);
   kicker naranjo en mayusculas + titulo 30 pt; circulos naranjos numerados; capturas con borde; notas del orador en cada diapositiva.
   Se genero con python-pptx y se reviso exportando a PNG con PowerPoint (COM).
7. Actualizar el prompt de estudio (para ChatGPT) con todo lo de Kafka.

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
