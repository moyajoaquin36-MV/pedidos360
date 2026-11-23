package cl.duocuc.pedidos360.productos.mensajeria.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** El topico se arma con los valores de application.yml (bloque kafka.topicos), sin broker. */
@SpringBootTest
class KafkaConfigTest {

    @Autowired
    private NewTopic topicoInventarioMovimientos;

    @Test
    void inventarioMovimientosEsCompactadoCon3Particiones3Replicas() {
        assertThat(topicoInventarioMovimientos.name()).isEqualTo("inventario-movimientos");
        assertThat(topicoInventarioMovimientos.numPartitions()).isEqualTo(3);
        assertThat(topicoInventarioMovimientos.replicationFactor()).isEqualTo((short) 3);
        assertThat(topicoInventarioMovimientos.configs())
                .containsEntry("cleanup.policy", "compact")
                .containsEntry("min.insync.replicas", "2")
                .doesNotContainKey("retention.ms");
    }
}
