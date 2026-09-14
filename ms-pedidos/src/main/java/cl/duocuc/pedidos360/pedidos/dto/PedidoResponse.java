package cl.duocuc.pedidos360.pedidos.dto;

import cl.duocuc.pedidos360.pedidos.domain.EstadoPedido;
import cl.duocuc.pedidos360.pedidos.domain.ModalidadEntrega;
import cl.duocuc.pedidos360.pedidos.domain.Pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoResponse(
        Long id,
        String localId,
        String clienteId,
        EstadoPedido estado,
        ModalidadEntrega modalidadEntrega,
        BigDecimal total,
        Instant fechaCreacion,
        Instant fechaActualizacion,
        List<ItemPedidoRequest> items) {

    public static PedidoResponse from(Pedido pedido) {
        List<ItemPedidoRequest> items = pedido.getItems().stream()
                .map(item -> new ItemPedidoRequest(
                        item.getProductoId(),
                        item.getNombreProducto(),
                        item.getCantidad(),
                        item.getPrecioUnitario()))
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getLocalId(),
                pedido.getClienteId(),
                pedido.getEstado(),
                pedido.getModalidadEntrega(),
                pedido.getTotal(),
                pedido.getFechaCreacion(),
                pedido.getFechaActualizacion(),
                items);
    }
}
