package cl.duocuc.pedidos360.auditoria.mensajeria.kafka.evento;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Linea de un PedidoEvento: producto, cantidad y precio al momento del pedido. */
public record ItemPedidoEvento(
        @NotNull Long productoId,
        String nombreProducto,
        @NotNull @Positive Integer cantidad,
        @PositiveOrZero BigDecimal precioUnitario) {
}
