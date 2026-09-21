/**
 * Tenant real: pedidos360dsy110736. App registrations: Pedidos360-Frontend (SPA)
 * y Pedidos360-API (recurso). redirectUri usa el origen actual para servir
 * tanto en localhost como en el dominio del API Gateway.
 */
export const environment = {
  production: false,

  azureAd: {
    clientId: '9c93580f-21b8-4cc4-a77f-61ee0f416e1d',
    tenantId: '0079c3b5-6d24-4e9a-ae29-879747fa5377',
    redirectUri: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
    // Scope expuesto por la API (Expose an API -> Add a scope) en el App
    // Registration del backend.
    apiScopes: ['api://69bfb4d4-1adf-46e5-933a-9e1c2be64a40/access_as_user']
  },

  // Todo el trafico al backend pasa por AWS API Gateway (HTTP API), que enruta
  // /api/pedidos -> ms-pedidos y /api/productos -> ms-productos en EC2.
  pedidosBaseUrl: 'https://ng3zai38m7.execute-api.us-east-1.amazonaws.com/api/pedidos',
  productosBaseUrl: 'https://ng3zai38m7.execute-api.us-east-1.amazonaws.com/api/productos'
};
