package cl.duocuc.pedidos360.pedidos.controller;

public class PedidoNotFoundException extends RuntimeException {
    public PedidoNotFoundException(Long id) {
        super("No existe el pedido con id " + id);
    }
}
