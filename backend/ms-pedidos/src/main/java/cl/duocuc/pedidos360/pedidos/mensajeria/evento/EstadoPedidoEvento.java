package cl.duocuc.pedidos360.pedidos.mensajeria.evento;

import java.time.Instant;

/** Mensaje publicado cuando un pedido cambia de estado; con el se notifica al cliente. */
public record EstadoPedidoEvento(
        Long pedidoId,
        String clienteId,
        String localId,
        String estado,
        Instant fecha) {
}
