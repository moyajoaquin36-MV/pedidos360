package cl.duocuc.pedidos360.rabbitadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Dead-letter exchange y sufijo que se usan al crear una cola con DLQ (application.yml). */
@ConfigurationProperties(prefix = "rabbit-admin")
public record RabbitAdminProperties(String deadLetterExchange, String sufijoDlq) {
}
