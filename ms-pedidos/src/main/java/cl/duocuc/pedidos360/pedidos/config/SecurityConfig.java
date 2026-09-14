package cl.duocuc.pedidos360.pedidos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Recursos protegidos con JWT emitido por Azure AD (Entra ID).
 *
 * El JwtDecoder se construye a partir del JWK Set URI (no del issuer-uri)
 * para evitar que Spring resuelva el documento de descubrimiento OIDC de
 * forma sincrona al levantar el contexto: asi la app compila y arranca
 * (incluidos los tests) aunque el tenant configurado no exista todavia.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AzureAdProperties azureAdProperties;

    public SecurityConfig(AzureAdProperties azureAdProperties) {
        this.azureAdProperties = azureAdProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/**")
                            .hasAnyRole("CLIENTE", "OPERADOR_COCINA", "REPARTIDOR", "ADMIN_LOCAL", "ADMIN_GENERAL")
                        .requestMatchers(HttpMethod.POST, "/api/pedidos")
                            .hasAnyRole("CLIENTE", "ADMIN_LOCAL", "ADMIN_GENERAL")
                        .requestMatchers(HttpMethod.PATCH, "/api/pedidos/*/estado")
                            .hasAnyRole("OPERADOR_COCINA", "REPARTIDOR", "ADMIN_LOCAL", "ADMIN_GENERAL")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(new AzureRolesJwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(azureAdProperties.jwkSetUri())
                .build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(azureAdProperties.issuerUri());
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(azureAdProperties.clientId());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));

        return decoder;
    }
}
