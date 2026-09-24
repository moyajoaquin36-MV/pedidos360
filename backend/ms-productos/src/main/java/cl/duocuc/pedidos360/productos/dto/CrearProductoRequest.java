package cl.duocuc.pedidos360.productos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CrearProductoRequest(
        @NotBlank String localId,
        @NotBlank String sku,
        @NotBlank String nombre,
        @NotNull @PositiveOrZero BigDecimal precio,
        @NotNull @PositiveOrZero Integer stock) {
}
