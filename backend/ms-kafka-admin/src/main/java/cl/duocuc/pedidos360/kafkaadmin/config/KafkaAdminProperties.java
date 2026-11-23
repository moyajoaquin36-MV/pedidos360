package cl.duocuc.pedidos360.kafkaadmin.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Ajustes del administrador (application.yml, prefijo "kafka-admin").
 *
 * timeout: espera maxima de cada operacion del AdminClient; si se cumple se
 *          responde 503 (Kafka no disponible) en vez de dejar colgada la peticion.
 */
@ConfigurationProperties(prefix = "kafka-admin")
public record KafkaAdminProperties(@DefaultValue("20s") Duration timeout) {
}
