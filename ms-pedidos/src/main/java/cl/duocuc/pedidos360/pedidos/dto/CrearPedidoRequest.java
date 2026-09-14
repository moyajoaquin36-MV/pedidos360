package cl.duocuc.pedidos360.pedidos.dto;

import cl.duocuc.pedidos360.pedidos.domain.ModalidadEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CrearPedidoRequest(
        @NotNull String localId,
        @NotNull ModalidadEntrega modalidadEntrega,
        @NotEmpty @Valid List<ItemPedidoRequest> items) {
}
