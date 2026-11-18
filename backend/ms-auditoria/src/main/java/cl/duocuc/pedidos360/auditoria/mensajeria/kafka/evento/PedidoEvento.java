package cl.duocuc.pedidos360.auditoria.mensajeria.kafka.evento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Evento del topico pedidos-eventos (docs/kafka-contrato.md, seccion 3.1).
 * tipo: PEDIDO_CREADO | ESTADO_CAMBIADO. En ESTADO_CAMBIADO, estado es el
 * nuevo y estadoAnterior el previo; items y total vienen siempre.
 */
public record PedidoEvento(
        @NotBlank String eventId,
        @NotBlank String tipo,
        @NotNull Instant fecha,
        @NotNull Long pedidoId,
        @NotBlank String localId,
        String clienteId,
        @NotBlank String estado,
        String estadoAnterior,
        String modalidadEntrega,
        @NotNull @PositiveOrZero BigDecimal total,
        List<@NotNull @Valid ItemPedidoEvento> items,
        @NotBlank String usuario,
        @NotBlank String rol) {
}
