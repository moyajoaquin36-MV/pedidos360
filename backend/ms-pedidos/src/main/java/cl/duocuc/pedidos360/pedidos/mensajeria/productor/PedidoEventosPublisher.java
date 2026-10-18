package cl.duocuc.pedidos360.pedidos.mensajeria.productor;

import cl.duocuc.pedidos360.pedidos.domain.Pedido;
import cl.duocuc.pedidos360.pedidos.mensajeria.config.MensajeriaProperties;
import cl.duocuc.pedidos360.pedidos.mensajeria.evento.EstadoPedidoEvento;
import cl.duocuc.pedidos360.pedidos.mensajeria.evento.PedidoCreadoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

/**
 * Productor de eventos de pedidos. Es lo unico que el controller conoce de
 * RabbitMQ: no sabe de exchanges ni routing keys.
 *
 * Publica despues del commit (no se emite un ticket de un pedido que hizo
 * rollback) y si el broker falla solo lo registra en el log: la mensajeria es
 * asincrona y no debe romper la creacion ni la actualizacion del pedido.
 */
@Component
public class PedidoEventosPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventosPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public PedidoEventosPublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void pedidoCreado(Pedido pedido) {
        PedidoCreadoEvento evento = new PedidoCreadoEvento(
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getLocalId(),
                pedido.getModalidadEntrega().name(),
                pedido.getItems().size(),
                pedido.getTotal(),
                Instant.now());
        publicarTrasCommit(props.exchanges().direct(), props.routingKeys().pedidoCreado(), evento);
    }

    public void estadoCambiado(Pedido pedido) {
        EstadoPedidoEvento evento = new EstadoPedidoEvento(
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getLocalId(),
                pedido.getEstado().name(),
                Instant.now());
        String routingKey = props.routingKeys().estadoPedido() + "." + pedido.getEstado().name();
        publicarTrasCommit(props.exchanges().topic(), routingKey, evento);
    }

    private void publicarTrasCommit(String exchange, String routingKey, Object evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publicar(exchange, routingKey, evento);
                }
            });
        } else {
            publicar(exchange, routingKey, evento);
        }
    }

    private void publicar(String exchange, String routingKey, Object evento) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, evento);
            log.info("Publicado en {} [{}]: {}", exchange, routingKey, evento);
        } catch (AmqpException e) {
            log.error("No se pudo publicar en {} [{}], el pedido no se ve afectado: {}", exchange, routingKey, evento, e);
        }
    }
}
