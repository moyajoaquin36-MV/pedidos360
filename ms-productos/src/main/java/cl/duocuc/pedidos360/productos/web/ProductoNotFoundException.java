package cl.duocuc.pedidos360.productos.web;

public class ProductoNotFoundException extends RuntimeException {
    public ProductoNotFoundException(Long id) {
        super("No existe el producto con id " + id);
    }
}
