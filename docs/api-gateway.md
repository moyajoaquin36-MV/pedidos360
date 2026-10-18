# AWS API Gateway (HTTP API) — Pedidos 360

El frontend nunca llama directo a los microservicios: todo pasa por el API Gateway,
que valida el JWT de Azure AD y reenvia la request al microservicio en la EC2.

```
Angular (MSAL)  --Bearer JWT-->  API Gateway  --valida iss/aud/firma-->  EC2 :8081 ms-pedidos
                                                                         EC2 :8082 ms-productos
```

## 1. Rutas (una por endpoint, sin `ANY /{proxy+}`)

| # | Ruta en el Gateway | Integracion (HTTP) | Rol que la usa |
|---|---|---|---|
| 1 | `GET /api/productos` | `http://<EC2>:8082/api/productos` | todos |
| 2 | `GET /api/productos/{id}` | `http://<EC2>:8082/api/productos/{id}` | todos |
| 3 | `POST /api/productos` | `http://<EC2>:8082/api/productos` | ADMIN_GENERAL |
| 4 | `PATCH /api/productos/{id}/stock` | `http://<EC2>:8082/api/productos/{id}/stock` | OPERADOR_COCINA, ADMIN_GENERAL |
| 5 | `PATCH /api/productos/{id}/reposicion` | `http://<EC2>:8082/api/productos/{id}/reposicion` | ADMIN_GENERAL |
| 6 | `GET /api/pedidos` | `http://<EC2>:8081/api/pedidos` | todos (cliente ve solo los suyos) |
| 7 | `GET /api/pedidos/{id}` | `http://<EC2>:8081/api/pedidos/{id}` | todos |
| 8 | `POST /api/pedidos` | `http://<EC2>:8081/api/pedidos` | CLIENTE, ADMIN_GENERAL |
| 9 | `PATCH /api/pedidos/{id}/estado` | `http://<EC2>:8081/api/pedidos/{id}/estado` | OPERADOR_COCINA, ADMIN_GENERAL |

**No se publica** `POST /api/productos/descuento-stock`: es una llamada **interna**
de ms-pedidos a ms-productos por la red de Docker (`http://ms-productos:8080`, ver
`ProductosClient.java`). Exponerla permitiria a cualquiera descontar stock sin crear pedido.

**Frontend**: `GET /` y `GET /{proxy+}` (sin authorizer) → nginx en `http://<EC2>:80`.
Son `GET` y no `ANY` a proposito: un `ANY /{proxy+}` captura tambien los `OPTIONS`
y el preflight terminaba en nginx (405) en vez de responderlo el Gateway.

**No se crean rutas `OPTIONS`**: el preflight de CORS lo responde el Gateway solo
(204, seccion 3). Si una ruta capturara `OPTIONS`, el preflight iria al backend o
al authorizer (401, porque el navegador no manda token en el preflight).

**IP de la EC2**: el Learner Lab cambia la IP publica cada vez que se reinicia el lab.
Si el Gateway responde 503, actualizar la IP en todas las integraciones.

## 2. JWT Authorizer (asociado a las 9 rutas)

| Campo | Valor |
|---|---|
| Tipo | JWT |
| Identity source | `$request.header.Authorization` |
| Issuer | `https://login.microsoftonline.com/0079c3b5-6d24-4e9a-ae29-879747fa5377/v2.0` |
| Audience | `69bfb4d4-1adf-46e5-933a-9e1c2be64a40` (client ID de la app **Pedidos360-API**) |
| Authorization scope (en cada ruta) | `access_as_user` |

Son los mismos valores que valida el backend (`AzureAdProperties.issuerUri()` y
`AudienceValidator`). **Comprobarlo con un token real** en https://jwt.ms:
`iss` y `aud` deben ser exactamente los de la tabla. Si `iss` sale como
`https://sts.windows.net/...` el token es v1: en el manifiesto de Pedidos360-API
poner `"accessTokenAcceptedVersion": 2`.

El Gateway valida **firma, expiracion, issuer, audience y scope**. Los roles
(claim `roles`) los vuelve a revisar cada microservicio en `SecurityConfig`.

## 3. CORS (solo en el Gateway)

| Campo | Valor |
|---|---|
| Access-Control-Allow-Origin | `https://ng3zai38m7.execute-api.us-east-1.amazonaws.com` (el frontend se sirve por el mismo Gateway, rutas `ANY /` y `ANY /{proxy+}`) y `http://localhost:4200` |
| Access-Control-Allow-Methods | `GET, POST, PATCH, OPTIONS` |
| Access-Control-Allow-Headers | `authorization, content-type` |
| Max age | `3600` |

Con CORS configurado, el HTTP API ignora los headers CORS que devuelva el backend.

## 4. Azure AD (Entra ID) — checklist

- **Pedidos360-Frontend → Authentication**: solo plataforma **Single-page application**
  (redirect URIs: URL del frontend y `http://localhost:4200`). Sin plataforma *Web*.
  *Implicit grant*: los dos checkbox **desmarcados** → queda solo Authorization Code + PKCE.
- **Pedidos360-API → App roles**: `CLIENTE`, `OPERADOR_COCINA`, `ADMIN_GENERAL`
  (`ADMIN_LOCAL` y `REPARTIDOR` quedaron desactivados: el sistema no los usa).
- **Enterprise applications → Pedidos360-API → Users and groups**: un usuario de prueba por rol:

  | Usuario | Rol |
  |---|---|
  | `cliente@pedidos360dsy110736.onmicrosoft.com` | CLIENTE |
  | `operador@pedidos360dsy110736.onmicrosoft.com` | OPERADOR_COCINA |
  | cuenta del alumno (invitada) | ADMIN_GENERAL |

  Un usuario sin rol asignado entra como CLIENTE (`AzureRolesJwtAuthenticationConverter`).
- **Flujo de usuario de registro** (sign up) asociado a Pedidos360-Frontend.

## 5. Evidencias con Postman

1. Importar `docs/postman/pedidos360.postman_collection.json`.
2. Iniciar sesion en el frontend → F12 → pestaña *Network* → cualquier request a
   `execute-api` → copiar el valor de `Authorization` sin la palabra `Bearer`.
3. Pegarlo en la variable `token` de la coleccion y ejecutar *Run collection*.
4. Repetir con el usuario de cada rol. Capturar: respuesta JSON de cada ruta,
   el 401 sin token, y un 403 (ej. CLIENTE haciendo `POST /api/productos`).
