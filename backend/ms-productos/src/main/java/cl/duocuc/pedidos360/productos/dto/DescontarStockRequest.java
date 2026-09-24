package cl.duocuc.pedidos360.productos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record DescontarStockRequest(@NotEmpty List<@Valid ItemDescuento> items) {

    public record ItemDescuento(@NotNull Long productoId, @NotNull @Positive Integer cantidad) {
    }
}
