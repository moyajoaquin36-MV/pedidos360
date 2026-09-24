# ms-productos

Microservicio de catalogo/inventario del sistema **Pedidos 360** (DSY1107 - Evaluacion Parcial N°1).
Expone `/api/productos`, protegido con JWT emitido por Azure AD (Entra ID) y consumido a traves de AWS API Gateway.

## Stack

- Java 21 + Spring Boot 4.1.1 (Web, Data JPA, OAuth2 Resource Server, Validation, Lombok)
- MySQL (configurable via variables de entorno)
- Tests con MockMvc + H2 en memoria (no requieren Azure ni MySQL reales)

## Configuracion pendiente antes de desplegar

Este repo se entrega con **placeholders**. Antes de correr contra el tenant real hay que definir estas variables de entorno (ver `src/main/resources/application.yml`):

| Variable | Descripcion |
|---|---|
| `AZURE_TENANT_ID` | Tenant ID del Azure AD creado (ver guia "Creacion de tenant") |
| `AZURE_CLIENT_ID` | Client ID (Application ID) de la app registrada en ese tenant |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Conexion a la instancia MySQL (EC2) |
| `SERVER_PORT` | Puerto del servicio (default `8082`) |

Sin estas variables el servicio **igual compila y levanta** (usa valores por defecto), pero cualquier
JWT real sera rechazado hasta configurar el tenant correcto.

## Correr localmente

```bash
./mvnw spring-boot:run
```

## Correr los tests

```bash
./mvnw test
```

## Seguridad

- `GET /api/productos` -> cualquier rol autenticado (catalogo visible para armar pedidos).
- `POST /api/productos` -> solo `ADMIN_LOCAL` / `ADMIN_GENERAL`.
- `PATCH /api/productos/{id}/stock` -> `OPERADOR_COCINA`, `ADMIN_LOCAL`, `ADMIN_GENERAL` (rebaja de
  inventario al preparar un pedido).
- Misma logica de validacion de JWT que `ms-pedidos` (issuer, audience, firma via JWK Set de Azure AD).
