package cl.duocuc.pedidos360.pedidos.mensajeria.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
 *  Cada cola tiene su DLQ: pedidos360.dlx --<cola>.dlq--> <cola>.dlq
 *  (un mensaje rechazado con NACK sin requeue cae en su DLQ).
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

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(props.exchanges().deadLetter()).durable(true).build();
    }

    // ---- Ruta 1: ticket del pedido (exchange direct) ----

    @Bean
    public Queue colaTickets() {
        return colaConDlq(props.colas().tickets());
    }

    @Bean
    public Queue colaTicketsDlq() {
        return QueueBuilder.durable(props.dlq(props.colas().tickets())).build();
    }

    @Bean
    public Binding bindingTickets() {
        return BindingBuilder.bind(colaTickets()).to(pedidosDirectExchange()).with(props.routingKeys().pedidoCreado());
    }

    @Bean
    public Binding bindingTicketsDlq() {
        return BindingBuilder.bind(colaTicketsDlq()).to(deadLetterExchange()).with(props.dlq(props.colas().tickets()));
    }

    // ---- Ruta 2: notificacion de cambio de estado (exchange topic) ----

    @Bean
    public Queue colaNotificaciones() {
        return colaConDlq(props.colas().notificaciones());
    }

    @Bean
    public Queue colaNotificacionesDlq() {
        return QueueBuilder.durable(props.dlq(props.colas().notificaciones())).build();
    }

    @Bean
    public Binding bindingNotificaciones() {
        return BindingBuilder.bind(colaNotificaciones()).to(pedidosTopicExchange()).with(props.routingKeys().estadoPedidoBinding());
    }

    @Bean
    public Binding bindingNotificacionesDlq() {
        return BindingBuilder.bind(colaNotificacionesDlq()).to(deadLetterExchange()).with(props.dlq(props.colas().notificaciones()));
    }

    // ---- Conversion y callbacks del productor ----

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /** Registra en logs los mensajes que el broker no pudo enrutar o no confirmo. */
    @Bean
    public RabbitTemplateCustomizer callbacksDeEntrega() {
        return template -> {
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

    private Queue colaConDlq(String nombre) {
        return QueueBuilder.durable(nombre)
                .deadLetterExchange(props.exchanges().deadLetter())
                .deadLetterRoutingKey(props.dlq(nombre))
                .build();
    }
}
