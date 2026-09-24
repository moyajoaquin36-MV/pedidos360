package cl.duocuc.pedidos360.productos.dto;

import cl.duocuc.pedidos360.productos.domain.Producto;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String localId,
        String sku,
        String nombre,
        BigDecimal precio,
        Integer stock) {

    public static ProductoResponse from(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getLocalId(),
                producto.getSku(),
                producto.getNombre(),
                producto.getPrecio(),
                producto.getStock());
    }
}
