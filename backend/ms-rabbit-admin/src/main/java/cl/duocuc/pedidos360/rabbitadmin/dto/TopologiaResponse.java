package cl.duocuc.pedidos360.rabbitadmin.dto;

/** Lo que aprovisiono POST /api/rabbitmq/topology para un dominio. */
public record TopologiaResponse(String exchange, String cola, String routingKey, String dlx, String dlq) {
}
