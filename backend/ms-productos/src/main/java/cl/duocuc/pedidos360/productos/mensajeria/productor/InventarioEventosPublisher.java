package cl.duocuc.pedidos360.productos.mensajeria.productor;

import cl.duocuc.pedidos360.productos.domain.Producto;
import cl.duocuc.pedidos360.productos.mensajeria.config.MensajeriaProperties;
import cl.duocuc.pedidos360.productos.mensajeria.evento.StockBajoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

/**
 * Productor de eventos de inventario. Es lo unico que el controller conoce de
 * RabbitMQ. Publica despues del commit y, si el broker falla, solo lo
 * registra: la rebaja de stock nunca falla por la mensajeria.
 */
@Component
public class InventarioEventosPublisher {

    private static final Logger log = LoggerFactory.getLogger(InventarioEventosPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public InventarioEventosPublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    /** Publica una alerta si el producto quedo en o bajo el umbral de stock. */
    public void revisarStockBajo(Producto producto) {
        if (producto.getStock() > props.umbralStockBajo()) {
            return;
        }
        StockBajoEvento evento = new StockBajoEvento(
                producto.getId(), producto.getNombre(), producto.getLocalId(), producto.getStock(), Instant.now());
        Runnable envio = () -> publicar(props.routingKeys().stockBajo(), evento);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    private void publicar(String routingKey, StockBajoEvento evento) {
        try {
            rabbitTemplate.convertAndSend(props.exchanges().topic(), routingKey, evento);
            log.info("Publicado en {} [{}]: {}", props.exchanges().topic(), routingKey, evento);
        } catch (AmqpException e) {
            log.error("No se pudo publicar la alerta de stock, el inventario no se ve afectado: {}", evento, e);
        }
    }
}
