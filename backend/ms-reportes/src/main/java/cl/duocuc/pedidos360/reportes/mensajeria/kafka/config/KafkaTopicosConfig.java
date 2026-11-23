package cl.duocuc.pedidos360.reportes.mensajeria.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declara los dos DLT (KafkaAdmin los crea al arrancar si no existen; si ya
 * existen no hace nada, por eso ms-auditoria puede declararlos tambien).
 * Los topicos principales los declaran sus productores (ms-pedidos y ms-productos).
 */
@Configuration
public class KafkaTopicosConfig {

    private final KafkaMensajeriaProperties props;

    public KafkaTopicosConfig(KafkaMensajeriaProperties props) {
        this.props = props;
    }

    @Bean
    public NewTopic pedidosEventosDlt() {
        return dlt(props.topicos().pedidosEventos());
    }

    @Bean
    public NewTopic inventarioMovimientosDlt() {
        return dlt(props.topicos().inventarioMovimientos());
    }

    private NewTopic dlt(String topico) {
        KafkaMensajeriaProperties.Dlt dlt = props.topicos().dlt();
        return TopicBuilder.name(props.dlt(topico))
                .partitions(dlt.particiones())
                .replicas(dlt.replicas())
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, dlt.cleanupPolicy())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(dlt.retentionMs()))
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(dlt.minInsyncReplicas()))
                .build();
    }
}
