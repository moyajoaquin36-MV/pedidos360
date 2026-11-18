package cl.duocuc.pedidos360.productos.mensajeria.kafka.evento;

/** Tipos de evento del topico inventario-movimientos. */
public enum TipoMovimiento {
    PRODUCTO_CREADO,
    STOCK_DESCONTADO,
    STOCK_REPUESTO
}
