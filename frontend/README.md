# pedidos360-frontend

Frontend Angular del sistema **Pedidos 360** (DSY1107 - Evaluacion Parcial N°1). Login corporativo
via Azure AD (MSAL) y consumo de los microservicios backend a traves de AWS API Gateway.

## Stack

- Angular 18 (standalone components, sin NgModules)
- `@azure/msal-angular` v3 + `@azure/msal-browser` v3

## Configuracion pendiente antes de desplegar

Editar `src/environments/environment.ts` con los valores reales una vez creado el tenant de Azure AD
y registrada la app SPA (ver guia "Creacion de tenant"):

| Campo | Descripcion |
|---|---|
| `azureAd.clientId` | Application (client) ID de la SPA registrada en Azure AD |
| `azureAd.tenantId` | Tenant ID del directorio creado |
| `azureAd.apiScopes` | Scope expuesto por el backend (`api://<client-id-backend>/access_as_user`) |
| `apiGatewayBaseUrl` | URL del AWS API Gateway una vez desplegado |

Sin estos valores la app **compila y corre igual**, pero el login contra Azure AD fallara hasta
configurar el tenant real.

## Correr localmente

```bash
npm install
npm start
```

## Tests

```bash
npm test
```

> En Windows sin Google Chrome instalado, Karma puede apuntar a Microsoft Edge (Chromium) asi:
> `CHROME_BIN="C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" npm test -- --browsers=ChromeHeadless`

## Estructura de autenticacion

- `app/auth/msal.config.ts`: factories de MSAL (instancia, guard, interceptor con `protectedResourceMap`
  apuntando al API Gateway).
- `app/auth/roles.ts`: helper para leer el claim `roles` del token (app roles de Azure AD:
  `CLIENTE`, `OPERADOR_COCINA`, `REPARTIDOR`, `ADMIN_LOCAL`, `ADMIN_GENERAL`).
- `app/app.config.ts`: registra `MsalService`, `MsalGuard`, `MsalInterceptor` (adjunta el token a toda
  llamada HTTP que matchee el API Gateway) y el `APP_INITIALIZER` que inicializa MSAL antes de usarlo.
- `app/pages/login`: pantalla publica con boton "Iniciar sesion" (`loginRedirect`).
- `app/pages/home`: pantalla protegida por `MsalGuard`, muestra la cuenta activa, sus roles, y una
  llamada de ejemplo a `ms-pedidos` via `PedidosService`.
