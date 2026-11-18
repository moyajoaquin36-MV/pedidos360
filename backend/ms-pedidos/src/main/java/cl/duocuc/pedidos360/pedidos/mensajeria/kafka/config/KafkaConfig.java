package cl.duocuc.pedidos360.pedidos.mensajeria.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Topologia Kafka de ms-pedidos: declara el topico que este servicio produce.
 * KafkaAdmin (auto-configurado por Spring Boot) lo crea al arrancar si no
 * existe; si ya existe no hace nada, por lo que declararlo es idempotente.
 *
 * El productor (KafkaTemplate) tambien lo auto-configura Spring Boot con las
 * propiedades de spring.kafka.producer (acks, idempotencia, tuning).
 */
@Configuration
public class KafkaConfig {

    private final KafkaTopicosProperties topicos;

    public KafkaConfig(KafkaTopicosProperties topicos) {
        this.topicos = topicos;
    }

    /** pedidos-eventos: historial de negocio (cleanup delete, retencion 7 dias). */
    @Bean
    public NewTopic topicoPedidosEventos() {
        return topico(topicos.pedidosEventos());
    }

    private NewTopic topico(KafkaTopicosProperties.Topico topico) {
        return TopicBuilder.name(topico.nombre())
                .partitions(topico.particiones())
                .replicas(topico.replicas())
                .configs(topico.configs())
                .build();
    }
}
