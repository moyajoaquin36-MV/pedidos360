export enum Rol {
  Cliente = 'CLIENTE',
  OperadorCocina = 'OPERADOR_COCINA',
  Repartidor = 'REPARTIDOR',
  AdminLocal = 'ADMIN_LOCAL',
  AdminGeneral = 'ADMIN_GENERAL'
}

/**
 * Azure AD (Entra ID) entrega los app roles asignados al usuario en el
 * claim "roles" del ID token / access token.
 */
export function obtenerRoles(idTokenClaims: Record<string, unknown> | undefined): string[] {
  if (!idTokenClaims) {
    return [];
  }
  const roles = idTokenClaims['roles'];
  return Array.isArray(roles) ? (roles as string[]) : [];
}
