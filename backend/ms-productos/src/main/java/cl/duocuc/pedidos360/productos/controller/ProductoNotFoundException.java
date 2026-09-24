package cl.duocuc.pedidos360.productos.controller;

public class ProductoNotFoundException extends RuntimeException {
    public ProductoNotFoundException(Long id) {
        super("No existe el producto con id " + id);
    }
}
