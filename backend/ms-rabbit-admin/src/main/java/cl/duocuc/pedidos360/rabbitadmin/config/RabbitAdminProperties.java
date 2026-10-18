package cl.duocuc.pedidos360.rabbitadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Convenciones de nombres del control plane (application.yml, prefijo "rabbit-admin").
 *
 * deadLetter: una cola "x" con DLQ obtiene "x.dlx" (fanout) y "x.dlq" (TTL = retencion).
 * topologia:  POST /api/rabbitmq/topology?dominio=orders crea orders.exchange,
 *             orders.queue (routing key orders.created), orders.dlx y orders.dlq.
 */
@ConfigurationProperties(prefix = "rabbit-admin")
public record RabbitAdminProperties(DeadLetter deadLetter, Topologia topologia) {

    public record DeadLetter(String sufijoDlx, String sufijoDlq, long ttlDlqMs) {
    }

    public record Topologia(String sufijoExchange, String sufijoCola, String sufijoRoutingKey) {
    }
}
