package cl.duocuc.pedidos360.rabbitadmin.service;

import cl.duocuc.pedidos360.rabbitadmin.config.RabbitAdminProperties;
import cl.duocuc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.ColaResponse;
import cl.duocuc.pedidos360.rabbitadmin.dto.CrearColaRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.CrearExchangeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.stereotype.Service;

/**
 * Toda la administracion de RabbitMQ pasa por aqui. El controller solo llama
 * a estos metodos y no conoce AmqpAdmin ni las clases de Spring AMQP.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;
    private final RabbitAdminProperties props;

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitAdminProperties props) {
        this.amqpAdmin = amqpAdmin;
        this.props = props;
    }

    /** Crea la cola; si lleva DLQ crea tambien "nombre.dlq" enlazada al dead-letter exchange. */
    public ColaResponse crearCola(CrearColaRequest request) {
        String nombre = request.nombre();
        if (amqpAdmin.getQueueInfo(nombre) != null) {
            throw new RecursoExistenteException("La cola '" + nombre + "' ya existe");
        }

        QueueBuilder cola = request.esDurable() ? QueueBuilder.durable(nombre) : QueueBuilder.nonDurable(nombre);
        String dlq = null;
        if (request.llevaDlq()) {
            dlq = nombre + props.sufijoDlq();
            amqpAdmin.declareExchange(ExchangeBuilder.directExchange(props.deadLetterExchange()).durable(true).build());
            Queue colaDlq = QueueBuilder.durable(dlq).build();
            amqpAdmin.declareQueue(colaDlq);
            amqpAdmin.declareBinding(enlace(props.deadLetterExchange(), dlq, dlq));
            cola.deadLetterExchange(props.deadLetterExchange()).deadLetterRoutingKey(dlq);
        }
        amqpAdmin.declareQueue(cola.build());
        log.info("Cola creada: {} (dlq: {})", nombre, dlq);
        return obtenerCola(nombre);
    }

    public ColaResponse obtenerCola(String nombre) {
        QueueInformation info = amqpAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        String dlq = nombre + props.sufijoDlq();
        return new ColaResponse(info.getName(), info.getMessageCount(), info.getConsumerCount(),
                amqpAdmin.getQueueInfo(dlq) != null ? dlq : null);
    }

    /** Elimina solo la cola indicada; su DLQ (si tiene) se elimina aparte. */
    public void eliminarCola(String nombre) {
        obtenerCola(nombre);
        amqpAdmin.deleteQueue(nombre);
        log.info("Cola eliminada: {}", nombre);
    }

    public void crearExchange(CrearExchangeRequest request) {
        amqpAdmin.declareExchange(new ExchangeBuilder(request.nombre(), request.tipo().valorRabbit())
                .durable(request.esDurable())
                .build());
        log.info("Exchange creado: {} ({})", request.nombre(), request.tipo());
    }

    public void eliminarExchange(String nombre) {
        amqpAdmin.deleteExchange(nombre);
        log.info("Exchange eliminado: {}", nombre);
    }

    public void crearBinding(BindingRequest request) {
        obtenerCola(request.cola());
        amqpAdmin.declareBinding(enlace(request.exchange(), request.cola(), request.routingKey()));
        log.info("Binding creado: {} --{}--> {}", request.exchange(), request.routingKey(), request.cola());
    }

    public void eliminarBinding(BindingRequest request) {
        amqpAdmin.removeBinding(enlace(request.exchange(), request.cola(), request.routingKey()));
        log.info("Binding eliminado: {} --{}--> {}", request.exchange(), request.routingKey(), request.cola());
    }

    private Binding enlace(String exchange, String cola, String routingKey) {
        return new Binding(cola, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }
}
