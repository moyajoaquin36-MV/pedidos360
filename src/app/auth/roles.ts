export enum Rol {
  Cliente = 'CLIENTE',
  OperadorCocina = 'OPERADOR_COCINA',
  Repartidor = 'REPARTIDOR',
  AdminLocal = 'ADMIN_LOCAL',
  AdminGeneral = 'ADMIN_GENERAL'
}

/**
 * Azure AD (Entra ID) entrega los app roles asignados al usuario en el
 * claim "roles" del token, pero solo respecto del recurso (audience) para
 * el que ese token fue emitido. El ID token tiene como audience la propia
 * SPA (sin roles asignados); el access token pedido para el scope de la
 * API tiene como audience la API (ahi es donde estan los roles reales).
 */
export function obtenerRoles(claims: Record<string, unknown> | undefined): string[] {
  if (!claims) {
    return [];
  }
  const roles = claims['roles'];
  return Array.isArray(roles) ? (roles as string[]) : [];
}

/** Decodifica el payload (segunda parte) de un JWT, sin validar la firma. */
export function decodificarPayloadJwt(token: string): Record<string, unknown> {
  const payload = token.split('.')[1];
  const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
  const json = decodeURIComponent(
    atob(base64)
      .split('')
      .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
      .join('')
  );
  return JSON.parse(json);
}
