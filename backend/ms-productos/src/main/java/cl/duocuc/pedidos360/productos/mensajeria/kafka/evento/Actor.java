package cl.duocuc.pedidos360.productos.mensajeria.kafka.evento;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Quien hizo la accion que genero el evento (lo pide el caso de uso de
 * auditoria). Se obtiene del JWT de Azure AD; en el descuento de stock es el
 * JWT del cliente que ms-pedidos reenvia.
 */
public record Actor(String usuario, String rol) {

    /** Mismo criterio que AzureRolesJwtAuthenticationConverter: sin roles asignados es CLIENTE. */
    private static final String ROL_POR_DEFECTO = "CLIENTE";

    /**
     * usuario: claim "name", si no "preferred_username", si no "sub".
     * rol: primer valor del claim "roles", si no CLIENTE.
     */
    public static Actor desde(Jwt jwt) {
        String usuario = primeroConTexto(
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("preferred_username"),
                jwt.getSubject());
        List<String> roles = jwt.getClaimAsStringList("roles");
        String rol = roles == null || roles.isEmpty() || !StringUtils.hasText(roles.getFirst())
                ? ROL_POR_DEFECTO
                : roles.getFirst();
        return new Actor(usuario, rol);
    }

    private static String primeroConTexto(String... valores) {
        for (String valor : valores) {
            if (StringUtils.hasText(valor)) {
                return valor;
            }
        }
        return null;
    }
}
