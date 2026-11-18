package cl.duocuc.pedidos360.productos.mensajeria.kafka.productor;

import cl.duocuc.pedidos360.productos.domain.Producto;
import cl.duocuc.pedidos360.productos.mensajeria.kafka.config.KafkaTopicosProperties;
import cl.duocuc.pedidos360.productos.mensajeria.kafka.evento.Actor;
import cl.duocuc.pedidos360.productos.mensajeria.kafka.evento.MovimientoInventarioEvento;
import cl.duocuc.pedidos360.productos.mensajeria.kafka.evento.TipoMovimiento;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Productor Kafka de movimientos de inventario (topico inventario-movimientos),
 * para reporteria y auditoria. Convive con InventarioEventosPublisher
 * (RabbitMQ, alertas de stock bajo): el controller llama a ambos.
 *
 * Igual que el de RabbitMQ: publica despues del commit y si Kafka falla solo lo
 * registra en el log; el stock nunca falla por Kafka.
 * Clave = productoId: los movimientos de un producto van a la misma particion
 * (en orden) y la compactacion conserva el ultimo de cada producto.
 */
@Component
public class InventarioKafkaPublisher {

    /** Header con el eventId; el consumidor lo pone en el MDC como correlationId. */
    public static final String HEADER_CORRELATION_ID = "correlation-id";

    private static final Logger log = LoggerFactory.getLogger(InventarioKafkaPublisher.class);

    private final KafkaTemplate<String, MovimientoInventarioEvento> kafkaTemplate;
    private final KafkaTopicosProperties topicos;

    public InventarioKafkaPublisher(KafkaTemplate<String, MovimientoInventarioEvento> kafkaTemplate,
                                    KafkaTopicosProperties topicos) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicos = topicos;
    }

    /** cantidad = stock inicial del producto. */
    public void productoCreado(Producto producto, Actor actor) {
        publicarTrasCommit(evento(TipoMovimiento.PRODUCTO_CREADO, producto, producto.getStock(), actor));
    }

    /** producto ya rebajado: su stock es el resultante. */
    public void stockDescontado(Producto producto, int cantidad, Actor actor) {
        publicarTrasCommit(evento(TipoMovimiento.STOCK_DESCONTADO, producto, cantidad, actor));
    }

    /** producto ya repuesto: su stock es el resultante. */
    public void stockRepuesto(Producto producto, int cantidad, Actor actor) {
        publicarTrasCommit(evento(TipoMovimiento.STOCK_REPUESTO, producto, cantidad, actor));
    }

    /** Se arma al momento del movimiento, con el stock que quedo tras aplicarlo. */
    private MovimientoInventarioEvento evento(TipoMovimiento tipo, Producto producto, Integer cantidad, Actor actor) {
        return new MovimientoInventarioEvento(
                UUID.randomUUID().toString(),
                tipo,
                Instant.now(),
                producto.getId(),
                producto.getLocalId(),
                producto.getSku(),
                producto.getNombre(),
                cantidad,
                producto.getStock(),
                actor.usuario(),
                actor.rol());
    }

    private void publicarTrasCommit(MovimientoInventarioEvento evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publicar(evento);
                }
            });
        } else {
            publicar(evento);
        }
    }

    private void publicar(MovimientoInventarioEvento evento) {
        String topico = topicos.inventarioMovimientos().nombre();
        ProducerRecord<String, MovimientoInventarioEvento> registro =
                new ProducerRecord<>(topico, String.valueOf(evento.productoId()), evento);
        registro.headers().add(HEADER_CORRELATION_ID, evento.eventId().getBytes(StandardCharsets.UTF_8));
        try {
            kafkaTemplate.send(registro).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("No se pudo publicar en {} (clave {}), el inventario no se ve afectado: {}",
                            topico, registro.key(), evento, error);
                } else {
                    RecordMetadata metadata = resultado.getRecordMetadata();
                    log.info("Publicado en {} particion={} offset={} clave={} [{}] eventId={}",
                            metadata.topic(), metadata.partition(), metadata.offset(),
                            registro.key(), evento.tipo(), evento.eventId());
                }
            });
        } catch (RuntimeException e) {
            log.error("No se pudo publicar en {} (clave {}), el inventario no se ve afectado: {}",
                    topico, registro.key(), evento, e);
        }
    }
}
