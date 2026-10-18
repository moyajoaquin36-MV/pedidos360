package cl.duocuc.pedidos360.productos.mensajeria.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
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
 * Topologia RabbitMQ de ms-productos. Toda la configuracion de mensajeria vive
 * aqui; el controller solo usa InventarioEventosPublisher.
 *
 * Ruta de mensajeria:
 *  3. Alerta de stock: pedidos360.topic --producto.stock.*--> inventario.alertas
 *  DLX fanout y DLQ: inventario.alertas.dlx --> inventario.alertas.dlq (TTL 24 h).
 *  (el exchange topic es el mismo que declara ms-pedidos).
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

    /** DLX fanout: reenvia todo mensaje muerto a la DLQ enlazada, sin routing key. */
    @Bean
    public FanoutExchange alertasStockDlx() {
        return ExchangeBuilder.fanoutExchange(props.dlx(props.colas().alertasStock())).durable(true).build();
    }

    @Bean
    public Queue colaAlertasStock() {
        String nombre = props.colas().alertasStock();
        return QueueBuilder.durable(nombre)
                .deadLetterExchange(props.dlx(nombre))
                .build();
    }

    /** DLQ (cuarentena): guarda el mensaje muerto durante el TTL configurado para revisarlo. */
    @Bean
    public Queue colaAlertasStockDlq() {
        return QueueBuilder.durable(props.dlq(props.colas().alertasStock()))
                .ttl((int) props.deadLetter().ttlDlqMs())
                .build();
    }

    @Bean
    public Binding bindingAlertasStock() {
        return BindingBuilder.bind(colaAlertasStock()).to(pedidosTopicExchange()).with(props.routingKeys().stockBinding());
    }

    @Bean
    public Binding bindingAlertasStockDlq() {
        return BindingBuilder.bind(colaAlertasStockDlq()).to(alertasStockDlx());
    }

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
}
