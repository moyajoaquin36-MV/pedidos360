package cl.duocuc.pedidos360.pedidos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ItemPedidoRequest(
        @NotNull Long productoId,
        @NotNull String nombreProducto,
        @NotNull @Positive Integer cantidad,
        @NotNull @Positive BigDecimal precioUnitario) {
}
