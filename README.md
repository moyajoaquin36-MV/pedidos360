# Pedidos 360 - Backend (2 microservicios Spring Boot)

## Arquitectura en una imagen

```
Navegador (Angular + MSAL)
        │  login con Azure AD  → obtiene id token + access token (JWT)
        ▼
AWS API Gateway  ── valida el JWT (issuer + audience de Azure AD)
   ├── /api/pedidos   ──►  ms-pedidos   ──►  BD ms_pedidos_db
   │                          │  (al crear un pedido descuenta stock)
   └── /api/productos ──►  ms-productos ──►  BD ms_productos_db
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

## Correr / probar
```bash
./mvnw test                       # tests de ambos microservicios
docker compose -f docker-compose.aws.yml --env-file .env up -d --build   # despliegue (EC2)
```
