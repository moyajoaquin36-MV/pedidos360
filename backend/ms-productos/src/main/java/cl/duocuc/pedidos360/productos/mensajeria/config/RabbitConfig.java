package cl.duocuc.pedidos360.productos.mensajeria.config;

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
 * Topologia RabbitMQ de ms-productos. Toda la configuracion de mensajeria vive
 * aqui; el controller solo usa InventarioEventosPublisher.
 *
 * Ruta de mensajeria:
 *  3. Alerta de stock: pedidos360.topic --producto.stock.*--> inventario.alertas
 *  DLQ: pedidos360.dlx --inventario.alertas.dlq--> inventario.alertas.dlq
 *  (el exchange topic y el dlx son los mismos que declara ms-pedidos).
 */
@Configuration
public class RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    private final MensajeriaProperties props;

    public RabbitConfig(MensajeriaProperties props) {
        this.props = props;
    }

    @Bean
    public TopicExchange pedidosTopicExchange() {
        return ExchangeBuilder.topicExchange(props.exchanges().topic()).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(props.exchanges().deadLetter()).durable(true).build();
    }

    @Bean
    public Queue colaAlertasStock() {
        String nombre = props.colas().alertasStock();
        return QueueBuilder.durable(nombre)
                .deadLetterExchange(props.exchanges().deadLetter())
                .deadLetterRoutingKey(props.dlq(nombre))
                .build();
    }

    @Bean
    public Queue colaAlertasStockDlq() {
        return QueueBuilder.durable(props.dlq(props.colas().alertasStock())).build();
    }

    @Bean
    public Binding bindingAlertasStock() {
        return BindingBuilder.bind(colaAlertasStock()).to(pedidosTopicExchange()).with(props.routingKeys().stockBinding());
    }

    @Bean
    public Binding bindingAlertasStockDlq() {
        return BindingBuilder.bind(colaAlertasStockDlq()).to(deadLetterExchange()).with(props.dlq(props.colas().alertasStock()));
    }

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
}
