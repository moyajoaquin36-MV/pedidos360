package cl.duocuc.pedidos360.productos.mensajeria.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges, colas y routing keys de RabbitMQ, todos definidos en
 * application.yml (prefijo "mensajeria"). Ninguna clase de negocio escribe
 * estos nombres a mano.
 */
@ConfigurationProperties(prefix = "mensajeria")
public record MensajeriaProperties(Exchanges exchanges, Colas colas, RoutingKeys routingKeys,
                                   String sufijoDlq, int umbralStockBajo) {

    public record Exchanges(String topic, String deadLetter) {
    }

    public record Colas(String alertasStock) {
    }

    /** stockBajo es la routing key con que se publica; stockBinding el patron topic de la cola. */
    public record RoutingKeys(String stockBajo, String stockBinding) {
    }

    /** Nombre de la DLQ de una cola; tambien es su routing key en el dead-letter exchange. */
    public String dlq(String cola) {
        return cola + sufijoDlq;
    }
}
