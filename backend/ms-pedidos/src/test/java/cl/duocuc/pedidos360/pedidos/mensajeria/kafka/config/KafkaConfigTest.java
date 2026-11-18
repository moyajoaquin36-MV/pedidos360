package cl.duocuc.pedidos360.pedidos.mensajeria.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** El topico se arma con los valores de application.yml (bloque kafka.topicos), sin broker. */
@SpringBootTest
class KafkaConfigTest {

    @Autowired
    private NewTopic topicoPedidosEventos;

    @Test
    void pedidosEventosTiene3Particiones3ReplicasYRetencionDe7Dias() {
        assertThat(topicoPedidosEventos.name()).isEqualTo("pedidos-eventos");
        assertThat(topicoPedidosEventos.numPartitions()).isEqualTo(3);
        assertThat(topicoPedidosEventos.replicationFactor()).isEqualTo((short) 3);
        assertThat(topicoPedidosEventos.configs())
                .containsEntry("cleanup.policy", "delete")
                .containsEntry("retention.ms", "604800000")
                .containsEntry("min.insync.replicas", "2");
    }
}
