/**
 * Configuracion de ejemplo. Reemplazar tenantId/clientId una vez creado el
 * tenant de Azure AD (ver guia "Creacion de tenant") y registrada la app
 * SPA en el Portal de Azure. apiGatewayBaseUrl debe apuntar al endpoint de
 * AWS API Gateway una vez desplegado.
 */
export const environment = {
  production: false,

  azureAd: {
    clientId: '00000000-0000-0000-0000-000000000000',
    tenantId: '00000000-0000-0000-0000-000000000000',
    redirectUri: 'http://localhost:4200',
    postLogoutRedirectUri: 'http://localhost:4200',
    // Scope expuesto por la API (Expose an API -> Add a scope) en el App
    // Registration del backend, ej: api://<client-id-backend>/access_as_user
    apiScopes: ['api://00000000-0000-0000-0000-000000000000/access_as_user']
  },

  apiGatewayBaseUrl: 'https://TU-API-ID.execute-api.TU-REGION.amazonaws.com/prod',

  api: {
    pedidos: '/api/pedidos',
    productos: '/api/productos'
  }
};
