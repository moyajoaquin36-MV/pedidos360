package cl.duocuc.pedidos360.pedidos.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AzureRolesJwtAuthenticationConverterTest {

    private final AzureRolesJwtAuthenticationConverter converter = new AzureRolesJwtAuthenticationConverter();

    private List<String> authorities(Jwt jwt) {
        return converter.convert(jwt).getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    private Jwt.Builder base() {
        return Jwt.withTokenValue("t").header("alg", "none").subject("u1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
    }

    @Test
    void mapeaLosRolesDeAzureConPrefijoRole() {
        assertEquals(List.of("ROLE_OPERADOR_COCINA"),
                authorities(base().claim("roles", List.of("OPERADOR_COCINA")).build()));
    }

    @Test
    void usuarioAutoregistradoSinRolesEsCliente() {
        assertEquals(List.of("ROLE_CLIENTE"), authorities(base().claim("name", "Nuevo").build()));
    }
}
