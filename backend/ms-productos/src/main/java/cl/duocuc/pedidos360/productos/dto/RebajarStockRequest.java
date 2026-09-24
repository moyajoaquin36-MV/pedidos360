package cl.duocuc.pedidos360.productos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RebajarStockRequest(@NotNull @Positive Integer cantidad) {
}
