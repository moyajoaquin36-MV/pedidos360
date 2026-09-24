# ms-pedidos

Microservicio de gestion de pedidos del sistema **Pedidos 360** (DSY1107 - Evaluacion Parcial N°1).
Expone `/api/pedidos`, protegido con JWT emitido por Azure AD (Entra ID) y consumido a traves de AWS API Gateway.

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
| `SERVER_PORT` | Puerto del servicio (default `8081`) |

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

Los tests usan un perfil H2 (`src/test/resources/application.yml`) y simulan el JWT de Azure AD con
`SecurityMockMvcRequestPostProcessors.jwt()`, sin necesitar conexion a Azure ni a MySQL.

## Seguridad

- `config/SecurityConfig`: cadena de filtros, RBAC por endpoint segun los roles del caso (`CLIENTE`,
  `OPERADOR_COCINA`, `REPARTIDOR`, `ADMIN_LOCAL`, `ADMIN_GENERAL`).
- `config/AzureRolesJwtAuthenticationConverter`: mapea el claim `roles` del token de Azure AD (donde
  vienen los *app roles* asignados al usuario) a `GrantedAuthority` con prefijo `ROLE_`.
- `config/AudienceValidator`: rechaza tokens que no fueron emitidos para este client-id.
- El `JwtDecoder` se arma con el JWK Set URI (no con `issuer-uri`) para no depender de resolver el
  documento de descubrimiento OIDC al levantar el contexto.
