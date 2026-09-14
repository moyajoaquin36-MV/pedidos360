package cl.duocuc.pedidos360.pedidos.dto;

import cl.duocuc.pedidos360.pedidos.domain.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoRequest(@NotNull EstadoPedido estado) {
}
