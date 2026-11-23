package cl.duocuc.pedidos360.auditoria.mensajeria.kafka.evento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/**
 * Evento del topico inventario-movimientos (docs/kafka-contrato.md, seccion 3.2).
 * tipo: PRODUCTO_CREADO | STOCK_DESCONTADO | STOCK_REPUESTO.
 * stockResultante es el stock despues del movimiento.
 */
public record MovimientoInventarioEvento(
        @NotBlank String eventId,
        @NotBlank String tipo,
        @NotNull Instant fecha,
        @NotNull Long productoId,
        @NotBlank String localId,
        String sku,
        @NotBlank String nombre,
        @NotNull @PositiveOrZero Integer cantidad,
        @NotNull @PositiveOrZero Integer stockResultante,
        @NotBlank String usuario,
        @NotBlank String rol) {
}
