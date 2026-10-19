package cl.duocuc.pedidos360.rabbitadmin.service;

import cl.duocuc.pedidos360.rabbitadmin.config.RabbitAdminProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RabbitAdminServiceTest {

    private final RabbitAdmin rabbitAdmin = mock(RabbitAdmin.class);
    private final RabbitAdminService service = new RabbitAdminService(rabbitAdmin, new RabbitAdminProperties(
            new RabbitAdminProperties.DeadLetter(".dlx", ".dlq", 86_400_000L),
            new RabbitAdminProperties.Topologia(".exchange", ".queue", ".created")));

    @Test
    void colaDeTopologiaUsaLaDlqDelDominio() {
        assertThat(service.nombreDlq("demo.queue")).isEqualTo("demo.dlq");
    }

    @Test
    void colaCreadaConPostQueuesUsaSuPropiaDlq() {
        assertThat(service.nombreDlq("pagos")).isEqualTo("pagos.dlq");
    }

    @Test
    void obtenerColaInformaSuDlq() {
        when(rabbitAdmin.getQueueInfo("demo.queue")).thenReturn(new QueueInformation("demo.queue", 3, 1));
        when(rabbitAdmin.getQueueInfo("demo.dlq")).thenReturn(new QueueInformation("demo.dlq", 0, 0));

        assertThat(service.obtenerCola("demo.queue").dlq()).isEqualTo("demo.dlq");
    }

    @Test
    void colaInexistenteDa404() {
        assertThatThrownBy(() -> service.obtenerCola("no.existe")).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
