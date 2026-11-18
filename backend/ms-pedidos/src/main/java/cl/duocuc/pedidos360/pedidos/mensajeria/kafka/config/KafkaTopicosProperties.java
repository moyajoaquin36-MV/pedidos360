package cl.duocuc.pedidos360.pedidos.mensajeria.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Topicos Kafka que produce ms-pedidos, definidos en application.yml (prefijo
 * "kafka.topicos"). Nombre, particiones, replicas y configs nunca se escriben
 * a mano en el codigo.
 */
@ConfigurationProperties(prefix = "kafka.topicos")
public record KafkaTopicosProperties(Topico pedidosEventos) {

    /** configs: propiedades del topico tal cual las entiende Kafka (cleanup.policy, retention.ms...). */
    public record Topico(String nombre, int particiones, int replicas, Map<String, String> configs) {
    }
}
