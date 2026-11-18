package cl.duocuc.pedidos360.pedidos.mensajeria.kafka.evento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Evento del topico pedidos-eventos (contrato en docs/kafka-contrato.md).
 * Es autosuficiente: lleva items y total aunque solo cambie el estado, para que
 * el consumidor no tenga que llamar a otro servicio.
 *
 * En ESTADO_CAMBIADO, estado es el nuevo y estadoAnterior el previo; en
 * PEDIDO_CREADO estadoAnterior va null. usuario y rol: quien hizo la accion.
 */
public record PedidoEvento(
        @NotBlank String eventId,
        @NotNull TipoEventoPedido tipo,
        @NotNull Instant fecha,
        @NotNull Long pedidoId,
        @NotBlank String localId,
        String clienteId,
        @NotBlank String estado,
        String estadoAnterior,
        String modalidadEntrega,
        @NotNull @PositiveOrZero BigDecimal total,
        List<Item> items,
        @NotBlank String usuario,
        @NotBlank String rol) {

    public record Item(Long productoId, String nombreProducto, Integer cantidad, BigDecimal precioUnitario) {
    }
}
