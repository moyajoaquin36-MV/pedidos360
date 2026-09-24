package cl.duocuc.pedidos360.productos.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private List<String> allowedOrigins;

    public SecurityConfig(AzureAdProperties azureAdProperties) {
        this.azureAdProperties = azureAdProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/productos/**")
                            .hasAnyRole("CLIENTE", "OPERADOR_COCINA", "ADMIN_GENERAL")
                        .requestMatchers(HttpMethod.POST, "/api/productos/descuento-stock")
                            .hasAnyRole("CLIENTE", "ADMIN_GENERAL")
                        .requestMatchers(HttpMethod.POST, "/api/productos")
                            .hasAnyRole("ADMIN_GENERAL")
                        .requestMatchers(HttpMethod.PATCH, "/api/productos/*/reposicion")
                            .hasAnyRole("ADMIN_GENERAL")
                        .requestMatchers(HttpMethod.PATCH, "/api/productos/*/stock")
                            .hasAnyRole("OPERADOR_COCINA", "ADMIN_GENERAL")
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
