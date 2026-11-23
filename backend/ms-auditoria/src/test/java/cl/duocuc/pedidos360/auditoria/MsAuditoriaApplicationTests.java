package cl.duocuc.pedidos360.auditoria;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Carga el contexto real (H2, sin broker): listeners registrados pero detenidos. */
@SpringBootTest
class MsAuditoriaApplicationTests {

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private List<NewTopic> topicos;

    @Test
    void listenersQuedanConfiguradosSegunElContrato() {
        for (String id : List.of("auditoria-pedidos-eventos", "auditoria-inventario-movimientos")) {
            MessageListenerContainer contenedor = registry.getListenerContainer(id);
            assertThat(contenedor).isInstanceOf(ConcurrentMessageListenerContainer.class);
            assertThat(contenedor.getGroupId()).isEqualTo("grupo-auditoria");
            // isRunning no sirve: al reanudar un contexto cacheado, Spring Test llama start() y Kafka arranca igual
            assertThat(contenedor.isAutoStartup()).isFalse();
            assertThat(((ConcurrentMessageListenerContainer<?, ?>) contenedor).getConcurrency()).isEqualTo(3);
            assertThat(contenedor.getContainerProperties().getAckMode()).isEqualTo(ContainerProperties.AckMode.RECORD);
            assertThat(((ConcurrentMessageListenerContainer<?, ?>) contenedor).getCommonErrorHandler())
                    .isInstanceOf(DefaultErrorHandler.class);
        }
    }

    @Test
    void declaraLosDosDlt() {
        assertThat(topicos).extracting(NewTopic::name)
                .containsExactlyInAnyOrder("pedidos-eventos.DLT", "inventario-movimientos.DLT");
    }
}
