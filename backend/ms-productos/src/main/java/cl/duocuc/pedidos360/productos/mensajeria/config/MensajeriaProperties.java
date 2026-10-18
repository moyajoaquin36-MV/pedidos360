package cl.duocuc.pedidos360.productos.mensajeria.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges, colas y routing keys de RabbitMQ, todos definidos en
 * application.yml (prefijo "mensajeria"). Ninguna clase de negocio escribe
 * estos nombres a mano.
 */
@ConfigurationProperties(prefix = "mensajeria")
public record MensajeriaProperties(Exchanges exchanges, Colas colas, RoutingKeys routingKeys,
                                   DeadLetter deadLetter, int umbralStockBajo) {

    public record Exchanges(String topic) {
    }

    public record Colas(String alertasStock) {
    }

    /** stockBajo es la routing key con que se publica; stockBinding el patron topic de la cola. */
    public record RoutingKeys(String stockBajo, String stockBinding) {
    }

    /**
     * Cada cola tiene su propio DLX (fanout) "cola.dlx" y su DLQ "cola.dlq".
     * ttlDlqMs es la politica de retencion: cuanto se guarda un mensaje muerto.
     */
    public record DeadLetter(String sufijoDlx, String sufijoDlq, long ttlDlqMs) {
    }

    public String dlx(String cola) {
        return cola + deadLetter.sufijoDlx();
    }

    public String dlq(String cola) {
        return cola + deadLetter.sufijoDlq();
    }
}
