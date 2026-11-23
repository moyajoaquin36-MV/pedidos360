package cl.duocuc.pedidos360.pedidos.mensajeria.kafka.productor;

import cl.duocuc.pedidos360.pedidos.domain.EstadoPedido;
import cl.duocuc.pedidos360.pedidos.domain.Pedido;
import cl.duocuc.pedidos360.pedidos.mensajeria.kafka.config.KafkaTopicosProperties;
import cl.duocuc.pedidos360.pedidos.mensajeria.kafka.evento.Actor;
import cl.duocuc.pedidos360.pedidos.mensajeria.kafka.evento.PedidoEvento;
import cl.duocuc.pedidos360.pedidos.mensajeria.kafka.evento.TipoEventoPedido;
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
import java.util.List;
import java.util.UUID;

/**
 * Productor Kafka de eventos de pedidos (topico pedidos-eventos), para
 * reporteria y auditoria. Convive con PedidoEventosPublisher (RabbitMQ): el
 * controller llama a ambos.
 *
 * Igual que el de RabbitMQ: publica despues del commit y si Kafka falla solo lo
 * registra en el log; el pedido nunca falla por Kafka.
 * Clave = pedidoId, asi todos los eventos de un pedido caen en la misma
 * particion y se leen en orden.
 */
@Component
public class PedidoKafkaPublisher {

    /** Header con el eventId; el consumidor lo pone en el MDC como correlationId. */
    public static final String HEADER_CORRELATION_ID = "correlation-id";

    private static final Logger log = LoggerFactory.getLogger(PedidoKafkaPublisher.class);

    private final KafkaTemplate<String, PedidoEvento> kafkaTemplate;
    private final KafkaTopicosProperties topicos;

    public PedidoKafkaPublisher(KafkaTemplate<String, PedidoEvento> kafkaTemplate, KafkaTopicosProperties topicos) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicos = topicos;
    }

    public void pedidoCreado(Pedido pedido, Actor actor) {
        publicarTrasCommit(evento(TipoEventoPedido.PEDIDO_CREADO, pedido, null, actor));
    }

    public void estadoCambiado(Pedido pedido, EstadoPedido estadoAnterior, Actor actor) {
        publicarTrasCommit(evento(TipoEventoPedido.ESTADO_CAMBIADO, pedido, estadoAnterior, actor));
    }

    /** Se arma dentro de la transaccion: items es lazy y tras el commit ya no se puede leer. */
    private PedidoEvento evento(TipoEventoPedido tipo, Pedido pedido, EstadoPedido estadoAnterior, Actor actor) {
        List<PedidoEvento.Item> items = pedido.getItems().stream()
                .map(item -> new PedidoEvento.Item(
                        item.getProductoId(), item.getNombreProducto(), item.getCantidad(), item.getPrecioUnitario()))
                .toList();
        return new PedidoEvento(
                UUID.randomUUID().toString(),
                tipo,
                Instant.now(),
                pedido.getId(),
                pedido.getLocalId(),
                pedido.getClienteId(),
                nombre(pedido.getEstado()),
                nombre(estadoAnterior),
                nombre(pedido.getModalidadEntrega()),
                pedido.getTotal(),
                items,
                actor.usuario(),
                actor.rol());
    }

    private void publicarTrasCommit(PedidoEvento evento) {
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

    private void publicar(PedidoEvento evento) {
        String topico = topicos.pedidosEventos().nombre();
        ProducerRecord<String, PedidoEvento> registro =
                new ProducerRecord<>(topico, String.valueOf(evento.pedidoId()), evento);
        registro.headers().add(HEADER_CORRELATION_ID, evento.eventId().getBytes(StandardCharsets.UTF_8));
        try {
            kafkaTemplate.send(registro).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("No se pudo publicar en {} (clave {}), el pedido no se ve afectado: {}",
                            topico, registro.key(), evento, error);
                } else {
                    RecordMetadata metadata = resultado.getRecordMetadata();
                    log.info("Publicado en {} particion={} offset={} clave={} [{}] eventId={}",
                            metadata.topic(), metadata.partition(), metadata.offset(),
                            registro.key(), evento.tipo(), evento.eventId());
                }
            });
        } catch (RuntimeException e) {
            log.error("No se pudo publicar en {} (clave {}), el pedido no se ve afectado: {}",
                    topico, registro.key(), evento, e);
        }
    }

    private static String nombre(Enum<?> valor) {
        return valor != null ? valor.name() : null;
    }
}
