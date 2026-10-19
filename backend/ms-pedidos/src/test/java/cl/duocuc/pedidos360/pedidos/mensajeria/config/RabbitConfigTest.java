package cl.duocuc.pedidos360.pedidos.mensajeria.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    private final RabbitConfig config = new RabbitConfig(new MensajeriaProperties(
            new MensajeriaProperties.Exchanges("pedidos360.direct", "pedidos360.topic"),
            new MensajeriaProperties.Colas("pedidos.tickets", "notificaciones.pedidos"),
            new MensajeriaProperties.RoutingKeys("pedido.creado", "pedido.estado", "pedido.estado.*"),
            new MensajeriaProperties.DeadLetter(".dlx", ".dlq", 86_400_000L)));

    @Test
    void colaPrincipalEsQuorumYApuntaASuDlx() {
        Queue cola = config.colaTickets();
        assertThat(cola.getArguments())
                .containsEntry("x-queue-type", "quorum")
                .containsEntry("x-dead-letter-exchange", "pedidos.tickets.dlx");
    }

    @Test
    void dlqEsQuorumConRetencionDe24Horas() {
        Queue dlq = config.colaTicketsDlq();
        assertThat(dlq.getName()).isEqualTo("pedidos.tickets.dlq");
        assertThat(dlq.getArguments())
                .containsEntry("x-queue-type", "quorum")
                .containsEntry("x-message-ttl", 86_400_000);
    }
}
