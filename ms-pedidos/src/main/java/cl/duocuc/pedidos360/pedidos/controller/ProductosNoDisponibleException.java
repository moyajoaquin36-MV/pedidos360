package cl.duocuc.pedidos360.pedidos.controller;

public class ProductosNoDisponibleException extends RuntimeException {
    public ProductosNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
