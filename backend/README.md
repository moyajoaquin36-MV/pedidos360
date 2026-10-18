# Pedidos 360 - Backend (3 microservicios Spring Boot + RabbitMQ)

## Arquitectura en una imagen

```
Navegador (Angular + MSAL)
        │  login con Azure AD  → obtiene id token + access token (JWT)
        ▼
AWS API Gateway  ── valida el JWT (issuer + audience de Azure AD)
   ├── /api/pedidos   ──►  ms-pedidos   ──►  BD ms_pedidos_db
   │                          │  (al crear un pedido descuenta stock)
   ├── /api/productos ──►  ms-productos ──►  BD ms_productos_db
   └── /api/rabbit    ──►  ms-rabbit-admin (crea/borra colas, exchanges, bindings)

ms-pedidos / ms-productos ──eventos──►  RabbitMQ (cluster rabbitmq1 + rabbitmq2)
        (todo corre en Docker sobre una EC2)
```

Cada microservicio **vuelve a validar** el JWT (firma, issuer, audience, expiracion) y aplica los roles.

## Donde esta cada cosa (por microservicio)

| Carpeta | Que hay |
|---|---|
| `controller/` | Endpoints REST (`PedidoController`, `ProductoController`) |
| `config/` | Seguridad: validacion del JWT de Azure AD y permisos por rol (`SecurityConfig`) |
| `domain/` | Entidades JPA (tablas) |
| `repository/` | Acceso a la base de datos |
| `dto/` | Objetos de entrada/salida de la API |
| `client/` | (`ms-pedidos`) llamada a `ms-productos` para descontar stock |

## Bases de datos: solo 2

`ms_pedidos_db` (pedido, item_pedido) y `ms_productos_db` (producto): **una por microservicio**.
Las otras que muestra MySQL (`mysql`, `sys`, `information_schema`, `performance_schema`) son
del sistema, no del proyecto. Ambas corren en un unico contenedor MySQL.

## Roles: solo 3, cada uno con una funcion

| Rol (Azure AD) | Que puede hacer |
|---|---|
| `CLIENTE` | Ve el catalogo, compra y ve **solo sus** pedidos |
| `OPERADOR_COCINA` | Ve **todos** los pedidos y avanza su estado (recibido → en preparacion → listo → entregado) |
| `ADMIN_GENERAL` | Todo lo anterior + agrega productos al catalogo |

## Mensajeria asincrona con RabbitMQ

Los pedidos y el inventario publican eventos; otros componentes los procesan despues,
sin frenar la respuesta al usuario. Si RabbitMQ falla, el pedido igual se crea (solo queda en el log).

| # | Cola (y su DLQ) | Exchange (tipo) | Routing key | Productor -> Consumidor | Para que |
|---|---|---|---|---|---|
| 1 | `pedidos.tickets` | `pedidos360.direct` (direct) | `pedido.creado` | ms-pedidos -> `TicketPedidoConsumer` | Generar el ticket del pedido |
| 2 | `notificaciones.pedidos` | `pedidos360.topic` (topic) | `pedido.estado.*` | ms-pedidos -> `NotificacionEstadoConsumer` | Avisar al cliente cada cambio de estado |
| 3 | `inventario.alertas` | `pedidos360.topic` (topic) | `producto.stock.*` | ms-productos -> `AlertaStockConsumer` | Alertar stock bajo (<= 5) |

Cada cola tiene `x-dead-letter-exchange = pedidos360.dlx` y su DLQ `<cola>.dlq`.

**ACK manual** (`ConfirmacionMensajes`): procesado bien -> `ack`; JSON invalido o datos incompletos -> `nack` sin requeue
(va directo a la DLQ, reintentar no sirve); otro error -> se reintenta **una** vez y si vuelve a fallar va a la DLQ.
Todo envio a DLQ queda en el log con el cuerpo del mensaje. Los mensajes que el broker no puede enrutar tambien se
registran (`publisher-returns` + `mandatory`).

**Donde esta** (mismo esquema en ms-pedidos y ms-productos):

| Paquete `mensajeria/` | Que hay |
|---|---|
| `config/` | `MensajeriaProperties` (nombres desde `application.yml`) y `RabbitConfig` (beans Queue/Exchange/Binding) |
| `evento/` | Los mensajes (records) |
| `productor/` | `PedidoEventosPublisher` / `InventarioEventosPublisher`: lo unico que usan los controllers |
| `consumidor/<dominio>/` | Los `@RabbitListener` agrupados por dominio (ticket, notificacion, alerta) |

**ms-rabbit-admin** (control plane, solo ADMIN_GENERAL): `POST/DELETE /api/rabbit/queues`, `GET /api/rabbit/queues/{nombre}`,
`POST/DELETE /api/rabbit/exchanges`, `POST/DELETE /api/rabbit/bindings`. Toda la logica esta en `RabbitAdminService`;
los DTO validan nombres (no vacios, sin el prefijo reservado `amq.`, tipo DIRECT/TOPIC/FANOUT).

**Cluster**: `rabbitmq1` + `rabbitmq2` en `docker-compose.aws.yml`, unidos por `deploy/rabbitmq/cluster.conf` y el mismo
Erlang cookie. Los microservicios se conectan a ambos (`RABBITMQ_ADDRESSES`). Consola: puerto 15672 (nodo 1) y 15673 (nodo 2).

## Correr / probar
```bash
./mvnw test                       # tests de ambos microservicios
docker compose -f docker-compose.aws.yml --env-file .env up -d --build   # despliegue (EC2)
```
