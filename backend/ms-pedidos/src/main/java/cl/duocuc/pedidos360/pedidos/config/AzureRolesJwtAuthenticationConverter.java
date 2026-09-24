package cl.duocuc.pedidos360.pedidos.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Azure AD (Entra ID) expone los roles de aplicacion asignados al usuario en
 * el claim "roles" del access token, a diferencia del claim "scope" que usa
 * Spring Security por defecto. Este converter mapea "roles" a
 * GrantedAuthority con prefijo ROLE_ para poder usar hasRole()/hasAnyRole().
 *
 * Un usuario que se registro solo (flujo de usuario de autoregistro) no tiene
 * app roles asignados: se le trata como CLIENTE. Los roles de personal
 * (OPERADOR_COCINA, ADMIN_GENERAL) los asigna un administrador en Azure AD.
 */
public class AzureRolesJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractRoles(jwt);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null || roles.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"));
        }
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }
}
