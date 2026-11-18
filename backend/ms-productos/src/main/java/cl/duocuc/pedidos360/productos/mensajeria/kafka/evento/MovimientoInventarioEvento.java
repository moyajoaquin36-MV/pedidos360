package cl.duocuc.pedidos360.productos.mensajeria.kafka.evento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/**
 * Evento del topico inventario-movimientos (contrato en docs/kafka-contrato.md).
 *
 * cantidad: unidades movidas (positivo); en PRODUCTO_CREADO es el stock inicial.
 * stockResultante: stock despues del movimiento, por eso el topico puede
 * compactarse por productoId sin perder el estado actual.
 * usuario y rol: quien hizo la accion.
 */
public record MovimientoInventarioEvento(
        @NotBlank String eventId,
        @NotNull TipoMovimiento tipo,
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
