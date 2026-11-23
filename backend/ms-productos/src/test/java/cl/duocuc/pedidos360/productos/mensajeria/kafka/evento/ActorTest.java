package cl.duocuc.pedidos360.productos.mensajeria.kafka.evento;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActorTest {

    private Jwt.Builder base() {
        return Jwt.withTokenValue("t").header("alg", "none").subject("sub-123")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
    }

    @Test
    void usaElClaimNameYElPrimerRol() {
        Jwt jwt = base().claim("name", "Joaquin Moya").claim("preferred_username", "jmoya@duocuc.cl")
                .claim("roles", List.of("ADMIN_GENERAL", "OPERADOR_COCINA")).build();

        assertThat(Actor.desde(jwt)).isEqualTo(new Actor("Joaquin Moya", "ADMIN_GENERAL"));
    }

    @Test
    void sinNameUsaPreferredUsername() {
        Jwt jwt = base().claim("preferred_username", "jmoya@duocuc.cl")
                .claim("roles", List.of("OPERADOR_COCINA")).build();

        assertThat(Actor.desde(jwt)).isEqualTo(new Actor("jmoya@duocuc.cl", "OPERADOR_COCINA"));
    }

    @Test
    void sinNameNiPreferredUsernameUsaSub() {
        Jwt jwt = base().claim("name", " ").claim("roles", List.of("CLIENTE")).build();

        assertThat(Actor.desde(jwt)).isEqualTo(new Actor("sub-123", "CLIENTE"));
    }

    @Test
    void usuarioAutoregistradoSinRolesEsCliente() {
        Jwt jwt = base().claim("name", "Nuevo").build();

        assertThat(Actor.desde(jwt)).isEqualTo(new Actor("Nuevo", "CLIENTE"));
    }
}
