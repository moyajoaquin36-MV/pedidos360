package cl.duocuc.pedidos360.pedidos.mensajeria.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges, colas y routing keys de RabbitMQ, todos definidos en
 * application.yml (prefijo "mensajeria"). Ninguna clase de negocio escribe
 * estos nombres a mano.
 */
@ConfigurationProperties(prefix = "mensajeria")
public record MensajeriaProperties(Exchanges exchanges, Colas colas, RoutingKeys routingKeys, String sufijoDlq) {

    public record Exchanges(String direct, String topic, String deadLetter) {
    }

    public record Colas(String tickets, String notificaciones) {
    }

    /**
     * estadoPedido es el prefijo con el que se publica (pedido.estado.LISTO);
     * estadoPedidoBinding es el patron topic con el que se enlaza la cola.
     */
    public record RoutingKeys(String pedidoCreado, String estadoPedido, String estadoPedidoBinding) {
    }

    /** Nombre de la DLQ de una cola; tambien es su routing key en el dead-letter exchange. */
    public String dlq(String cola) {
        return cola + sufijoDlq;
    }
}
