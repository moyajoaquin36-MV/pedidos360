package cl.duocuc.pedidos360.pedidos.mensajeria.evento;

import java.math.BigDecimal;
import java.time.Instant;

/** Mensaje publicado al crear un pedido; con el se genera el ticket. */
public record PedidoCreadoEvento(
        Long pedidoId,
        String clienteId,
        String localId,
        String modalidadEntrega,
        int cantidadItems,
        BigDecimal total,
        Instant fecha) {
}
