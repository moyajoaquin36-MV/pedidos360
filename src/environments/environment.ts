/**
 * Tenant real: pedidos360dsy110736 (creado 16/09/2026).
 * App registrations: Pedidos360-Frontend (SPA) y Pedidos360-API (recurso).
 *
 * pedidosBaseUrl / productosBaseUrl apuntan a localhost mientras no exista
 * el API Gateway de AWS: una vez desplegado, ambos deberian colapsar a la
 * misma URL base del Gateway (con distinto path, /api/pedidos vs
 * /api/productos), que es como quedaria en produccion.
 */
export const environment = {
  production: false,

  azureAd: {
    clientId: '9c93580f-21b8-4cc4-a77f-61ee0f416e1d',
    tenantId: '0079c3b5-6d24-4e9a-ae29-879747fa5377',
    redirectUri: 'http://localhost:4200',
    postLogoutRedirectUri: 'http://localhost:4200',
    // Scope expuesto por la API (Expose an API -> Add a scope) en el App
    // Registration del backend.
    apiScopes: ['api://69bfb4d4-1adf-46e5-933a-9e1c2be64a40/access_as_user']
  },

  // TEMPORAL: sin API Gateway todavia, se apunta directo a cada microservicio local.
  pedidosBaseUrl: 'http://localhost:8081/api/pedidos',
  productosBaseUrl: 'http://localhost:8082/api/productos'
};
