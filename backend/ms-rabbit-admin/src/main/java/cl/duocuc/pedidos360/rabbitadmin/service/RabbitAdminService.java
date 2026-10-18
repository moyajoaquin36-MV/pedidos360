package cl.duocuc.pedidos360.rabbitadmin.service;

import cl.duocuc.pedidos360.rabbitadmin.config.RabbitAdminProperties;
import cl.duocuc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.ColaResponse;
import cl.duocuc.pedidos360.rabbitadmin.dto.CrearColaRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.CrearExchangeRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.PurgaResponse;
import cl.duocuc.pedidos360.rabbitadmin.dto.TopologiaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

/**
 * Toda la administracion de RabbitMQ pasa por aqui (RabbitAdmin). El
 * controller solo llama a estos metodos y no conoce las clases de Spring AMQP.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final RabbitAdmin rabbitAdmin;
    private final RabbitAdminProperties props;

    public RabbitAdminService(RabbitAdmin rabbitAdmin, RabbitAdminProperties props) {
        this.rabbitAdmin = rabbitAdmin;
        this.props = props;
    }

    // ---- Colas ----

    /** Crea la cola; si lleva DLQ crea tambien "nombre.dlx" (fanout) y "nombre.dlq". */
    public ColaResponse crearCola(CrearColaRequest request) {
        String nombre = request.nombre();
        exigirQueNoExista(nombre);
        if (request.llevaDlq()) {
            declararColaConDlq(nombre, nombre + props.deadLetter().sufijoDlx(), nombre + props.deadLetter().sufijoDlq(),
                    request.esDurable());
        } else {
            rabbitAdmin.declareQueue(request.esDurable() ? QueueBuilder.durable(nombre).build() : QueueBuilder.nonDurable(nombre).build());
        }
        log.info("Cola creada: {} (con DLQ: {})", nombre, request.llevaDlq());
        return obtenerCola(nombre);
    }

    public ColaResponse obtenerCola(String nombre) {
        QueueInformation info = buscarCola(nombre);
        String dlq = nombre + props.deadLetter().sufijoDlq();
        return new ColaResponse(info.getName(), info.getMessageCount(), info.getConsumerCount(),
                rabbitAdmin.getQueueInfo(dlq) != null ? dlq : null);
    }

    /** Elimina solo la cola indicada; su DLQ (si tiene) se elimina aparte. */
    public void eliminarCola(String nombre) {
        buscarCola(nombre);
        rabbitAdmin.deleteQueue(nombre);
        log.info("Cola eliminada: {}", nombre);
    }

    /** Vacia la cola sin borrarla (por ejemplo, una DLQ ya revisada). */
    public PurgaResponse purgarCola(String nombre) {
        buscarCola(nombre);
        int eliminados = rabbitAdmin.purgeQueue(nombre);
        log.info("Cola purgada: {} ({} mensajes eliminados)", nombre, eliminados);
        return new PurgaResponse(nombre, eliminados);
    }

    // ---- Exchanges ----

    public void crearExchange(CrearExchangeRequest request) {
        rabbitAdmin.declareExchange(new ExchangeBuilder(request.nombre(), request.tipo().valorRabbit())
                .durable(request.esDurable())
                .build());
        log.info("Exchange creado: {} ({})", request.nombre(), request.tipo());
    }

    public void eliminarExchange(String nombre) {
        rabbitAdmin.deleteExchange(nombre);
        log.info("Exchange eliminado: {}", nombre);
    }

    // ---- Bindings ----

    public void crearBinding(BindingRequest request) {
        buscarCola(request.cola());
        rabbitAdmin.declareBinding(enlace(request.exchange(), request.cola(), request.routingKey()));
        log.info("Binding creado: {} --{}--> {}", request.exchange(), request.routingKey(), request.cola());
    }

    public void eliminarBinding(BindingRequest request) {
        rabbitAdmin.removeBinding(enlace(request.exchange(), request.cola(), request.routingKey()));
        log.info("Binding eliminado: {} --{}--> {}", request.exchange(), request.routingKey(), request.cola());
    }

    // ---- Topologia completa de un dominio ----

    /**
     * Aprovisiona en un paso exchange direct + cola + binding + DLX + DLQ:
     * primero la cuarentena (DLX/DLQ) y despues la ruta principal.
     */
    public TopologiaResponse crearTopologia(String dominio) {
        RabbitAdminProperties.Topologia t = props.topologia();
        String exchange = dominio + t.sufijoExchange();
        String cola = dominio + t.sufijoCola();
        String routingKey = dominio + t.sufijoRoutingKey();
        String dlx = dominio + props.deadLetter().sufijoDlx();
        String dlq = dominio + props.deadLetter().sufijoDlq();
        exigirQueNoExista(cola);

        Queue principal = declararColaConDlq(cola, dlx, dlq, true);
        DirectExchange exchangePrincipal = ExchangeBuilder.directExchange(exchange).durable(true).build();
        rabbitAdmin.declareExchange(exchangePrincipal);
        rabbitAdmin.declareBinding(BindingBuilder.bind(principal).to(exchangePrincipal).with(routingKey));

        log.info("Topologia creada para '{}': {} --{}--> {} | {} --> {}", dominio, exchange, routingKey, cola, dlx, dlq);
        return new TopologiaResponse(exchange, cola, routingKey, dlx, dlq);
    }

    // ---- Apoyo ----

    private Queue declararColaConDlq(String cola, String dlx, String dlq, boolean durable) {
        FanoutExchange exchangeDlx = ExchangeBuilder.fanoutExchange(dlx).durable(true).build();
        Queue colaDlq = QueueBuilder.durable(dlq).ttl((int) props.deadLetter().ttlDlqMs()).build();
        rabbitAdmin.declareExchange(exchangeDlx);
        rabbitAdmin.declareQueue(colaDlq);
        rabbitAdmin.declareBinding(BindingBuilder.bind(colaDlq).to(exchangeDlx));

        Queue principal = (durable ? QueueBuilder.durable(cola) : QueueBuilder.nonDurable(cola))
                .deadLetterExchange(dlx)
                .build();
        rabbitAdmin.declareQueue(principal);
        return principal;
    }

    private QueueInformation buscarCola(String nombre) {
        QueueInformation info = rabbitAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        return info;
    }

    private void exigirQueNoExista(String nombre) {
        if (rabbitAdmin.getQueueInfo(nombre) != null) {
            throw new RecursoExistenteException("La cola '" + nombre + "' ya existe");
        }
    }

    private Binding enlace(String exchange, String cola, String routingKey) {
        return new Binding(cola, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }
}
