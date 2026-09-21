package cl.duocuc.pedidos360.pedidos.web;

public class ProductosNoDisponibleException extends RuntimeException {
    public ProductosNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
