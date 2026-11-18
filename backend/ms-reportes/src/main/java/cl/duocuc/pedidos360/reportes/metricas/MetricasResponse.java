package cl.duocuc.pedidos360.reportes.metricas;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Foto del panel de operaciones.
 * ventasTotales y ventasPorLocal no cuentan pedidos CANCELADO; topProductos tampoco.
 */
public record MetricasResponse(
        long totalPedidos,
        BigDecimal ventasTotales,
        Map<String, Long> pedidosPorEstado,
        Map<String, BigDecimal> ventasPorLocal,
        List<ProductoDemandado> topProductos,
        List<StockCritico> stockCritico,
        List<EventoReciente> ultimosEventos,
        long eventosProcesados,
        Instant actualizado) {

    /** Unidades pedidas de un producto (suma de los items de los pedidos vigentes). */
    public record ProductoDemandado(Long productoId, String nombreProducto, long unidades) {
    }

    /** Producto cuyo ultimo stock conocido esta en o bajo el umbral configurado. */
    public record StockCritico(Long productoId, String localId, String sku, String nombre, int stock) {
    }

    /** Linea del feed de actividad reciente del panel. */
    public record EventoReciente(String eventId, String tipo, Instant fecha, String localId,
                                 String descripcion, String usuario, String rol) {
    }
}
