package cl.duocuc.pedidos360.productos.mensajeria.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Topicos Kafka que produce ms-productos, definidos en application.yml (prefijo
 * "kafka.topicos"). Nombre, particiones, replicas y configs nunca se escriben
 * a mano en el codigo.
 */
@ConfigurationProperties(prefix = "kafka.topicos")
public record KafkaTopicosProperties(Topico inventarioMovimientos) {

    /** configs: propiedades del topico tal cual las entiende Kafka (cleanup.policy, min.insync.replicas...). */
    public record Topico(String nombre, int particiones, int replicas, Map<String, String> configs) {
    }
}
