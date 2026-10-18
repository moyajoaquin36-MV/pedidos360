package cl.duocuc.pedidos360.productos.mensajeria.evento;

import java.time.Instant;

/** Mensaje publicado cuando un producto queda con stock bajo el umbral. */
public record StockBajoEvento(
        Long productoId,
        String nombre,
        String localId,
        Integer stock,
        Instant fecha) {
}
