package cl.duocuc.pedidos360.pedidos.mensajeria.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ de ms-pedidos. Toda la configuracion de mensajeria vive
 * aqui; el controller solo usa PedidoEventosPublisher.
 *
 * Rutas de mensajeria:
 *  1. Ticket:        pedidos360.direct --pedido.creado-->    pedidos.tickets
 *  2. Notificacion:  pedidos360.topic  --pedido.estado.*-->  notificaciones.pedidos
 *  Cada cola tiene su DLX fanout y su DLQ: <cola>.dlx --> <cola>.dlq (TTL 24 h).
 *  Un mensaje rechazado con NACK sin requeue cae en su DLQ y ahi queda para revisarlo.
 */
@Configuration
public class RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    private final MensajeriaProperties props;

    public RabbitConfig(MensajeriaProperties props) {
        this.props = props;
    }

    // ---- Exchanges ----

    @Bean
    public DirectExchange pedidosDirectExchange() {
        return ExchangeBuilder.directExchange(props.exchanges().direct()).durable(true).build();
    }

    @Bean
    public TopicExchange pedidosTopicExchange() {
        return ExchangeBuilder.topicExchange(props.exchanges().topic()).durable(true).build();
    }

    // ---- Ruta 1: ticket del pedido (exchange direct) ----

    @Bean
    public Queue colaTickets() {
        return colaConDlq(props.colas().tickets());
    }

    @Bean
    public FanoutExchange ticketsDlx() {
        return dlx(props.colas().tickets());
    }

    @Bean
    public Queue colaTicketsDlq() {
        return dlq(props.colas().tickets());
    }

    @Bean
    public Binding bindingTickets() {
        return BindingBuilder.bind(colaTickets()).to(pedidosDirectExchange()).with(props.routingKeys().pedidoCreado());
    }

    @Bean
    public Binding bindingTicketsDlq() {
        return BindingBuilder.bind(colaTicketsDlq()).to(ticketsDlx());
    }

    // ---- Ruta 2: notificacion de cambio de estado (exchange topic) ----

    @Bean
    public Queue colaNotificaciones() {
        return colaConDlq(props.colas().notificaciones());
    }

    @Bean
    public FanoutExchange notificacionesDlx() {
        return dlx(props.colas().notificaciones());
    }

    @Bean
    public Queue colaNotificacionesDlq() {
        return dlq(props.colas().notificaciones());
    }

    @Bean
    public Binding bindingNotificaciones() {
        return BindingBuilder.bind(colaNotificaciones()).to(pedidosTopicExchange()).with(props.routingKeys().estadoPedidoBinding());
    }

    @Bean
    public Binding bindingNotificacionesDlq() {
        return BindingBuilder.bind(colaNotificacionesDlq()).to(notificacionesDlx());
    }

    // ---- Conversion y callbacks del productor ----

    /**
     * El productor envia JSON y registra en logs los mensajes que el broker no
     * pudo enrutar o no confirmo. El convertidor JSON va SOLO en el RabbitTemplate:
     * si fuera un @Bean, Spring tambien lo usaria en los listeners y rechazaria un
     * JSON invalido antes de llegar a ConfirmacionMensajes (sin ACK manual ni log).
     */
    @Bean
    public RabbitTemplateCustomizer productorJson() {
        return template -> {
            template.setMessageConverter(new JacksonJsonMessageConverter());
            template.setReturnsCallback(returned -> log.error(
                    "Mensaje NO entregado (sin cola destino): exchange={} routingKey={} motivo={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
            template.setConfirmCallback((correlation, ack, cause) -> {
                if (!ack) {
                    log.error("El broker no confirmo el mensaje: {}", cause);
                }
            });
        };
    }

    /** Cola principal: los mensajes rechazados (NACK sin requeue) se van a su DLX. */
    private Queue colaConDlq(String nombre) {
        return QueueBuilder.durable(nombre)
                .deadLetterExchange(props.dlx(nombre))
                .build();
    }

    /** DLX fanout: reenvia todo mensaje muerto a la DLQ enlazada, sin routing key. */
    private FanoutExchange dlx(String cola) {
        return ExchangeBuilder.fanoutExchange(props.dlx(cola)).durable(true).build();
    }

    /** DLQ (cuarentena): guarda el mensaje muerto durante el TTL configurado para revisarlo. */
    private Queue dlq(String cola) {
        return QueueBuilder.durable(props.dlq(cola)).ttl((int) props.deadLetter().ttlDlqMs()).build();
    }
}
