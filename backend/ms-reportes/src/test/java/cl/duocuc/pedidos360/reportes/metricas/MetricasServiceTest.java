package cl.duocuc.pedidos360.reportes.metricas;

import cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento.ItemPedidoEvento;
import cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento.MovimientoInventarioEvento;
import cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento.PedidoEvento;
import cl.duocuc.pedidos360.reportes.metricas.MetricasResponse.EventoReciente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

class MetricasServiceTest {

    private static final Instant T0 = Instant.parse("2026-11-17T22:30:00Z");

    private final MetricasService service = new MetricasService(5);

    @Test
    void eventoRepetidoNoDuplicaMetricas() {
        PedidoEvento creado = creado("e1", 9L, "local-01", "3000.00", T0, item(1L, "Pan amasado", 2));

        assertThat(service.registrarPedido(creado)).isTrue();
        assertThat(service.registrarPedido(creado)).isFalse();

        MetricasResponse m = service.obtenerMetricas();
        assertThat(m.totalPedidos()).isEqualTo(1);
        assertThat(m.ventasTotales()).isEqualByComparingTo("3000.00");
        assertThat(m.eventosProcesados()).isEqualTo(1);
        assertThat(m.topProductos()).singleElement()
                .satisfies(p -> assertThat(p.unidades()).isEqualTo(2));
        assertThat(m.ultimosEventos()).hasSize(1);
    }

    @Test
    void cambioDeEstadoMueveElPedidoDeEstado() {
        service.registrarPedido(creado("e1", 9L, "local-01", "3000.00", T0, item(1L, "Pan amasado", 2)));
        service.registrarPedido(cambio("e2", 9L, "RECIBIDO", "EN_PREPARACION", T0.plusSeconds(60)));

        MetricasResponse m = service.obtenerMetricas();
        assertThat(m.totalPedidos()).isEqualTo(1);
        assertThat(m.pedidosPorEstado()).containsExactly(Map.entry("EN_PREPARACION", 1L));
        assertThat(m.ventasTotales()).isEqualByComparingTo("3000.00");
    }

    @Test
    void eventoAntiguoNoPisaElEstadoMasNuevo() {
        service.registrarPedido(cambio("e2", 9L, "RECIBIDO", "LISTO", T0.plusSeconds(60)));
        service.registrarPedido(creado("e1", 9L, "local-01", "3000.00", T0, item(1L, "Pan amasado", 2)));

        assertThat(service.obtenerMetricas().pedidosPorEstado()).containsExactly(Map.entry("LISTO", 1L));
    }

    @Test
    void pedidoCanceladoNoSumaVentas() {
        service.registrarPedido(creado("e1", 9L, "local-01", "3000.00", T0, item(1L, "Pan amasado", 2)));
        service.registrarPedido(creado("e2", 10L, "local-02", "1000.00", T0, item(2L, "Kuchen", 1)));
        service.registrarPedido(cambio("e3", 10L, "RECIBIDO", "CANCELADO", T0.plusSeconds(30)));

        MetricasResponse m = service.obtenerMetricas();
        assertThat(m.totalPedidos()).isEqualTo(2);
        assertThat(m.ventasTotales()).isEqualByComparingTo("3000.00");
        assertThat(m.ventasPorLocal()).containsOnlyKeys("local-01");
        assertThat(m.topProductos()).extracting(MetricasResponse.ProductoDemandado::productoId).containsExactly(1L);
    }

    @Test
    void ventasPorLocalYTopProductosOrdenados() {
        service.registrarPedido(creado("e1", 1L, "local-01", "3000.00", T0, item(1L, "Pan", 2), item(2L, "Kuchen", 5)));
        service.registrarPedido(creado("e2", 2L, "local-01", "500.00", T0, item(3L, "Te", 1)));
        service.registrarPedido(creado("e3", 3L, "local-02", "1500.00", T0, item(1L, "Pan", 4)));

        MetricasResponse m = service.obtenerMetricas();
        assertThat(m.ventasPorLocal().get("local-01")).isEqualByComparingTo("3500.00");
        assertThat(m.ventasPorLocal().get("local-02")).isEqualByComparingTo("1500.00");
        assertThat(m.topProductos()).extracting(MetricasResponse.ProductoDemandado::productoId).containsExactly(1L, 2L, 3L);
        assertThat(m.topProductos().getFirst().unidades()).isEqualTo(6);
    }

    @Test
    void stockCriticoUsaElUltimoStockYElUmbral() {
        service.registrarMovimiento(movimiento("m1", 1L, "PRODUCTO_CREADO", 10, 10, T0));
        service.registrarMovimiento(movimiento("m2", 1L, "STOCK_DESCONTADO", 6, 4, T0.plusSeconds(10)));
        service.registrarMovimiento(movimiento("m3", 2L, "PRODUCTO_CREADO", 5, 5, T0));
        service.registrarMovimiento(movimiento("m4", 3L, "PRODUCTO_CREADO", 20, 20, T0));

        assertThat(service.obtenerMetricas().stockCritico())
                .extracting(MetricasResponse.StockCritico::productoId, MetricasResponse.StockCritico::stock)
                .containsExactly(tuple(1L, 4), tuple(2L, 5));

        service.registrarMovimiento(movimiento("m5", 1L, "STOCK_REPUESTO", 30, 34, T0.plusSeconds(20)));
        assertThat(service.obtenerMetricas().stockCritico())
                .extracting(MetricasResponse.StockCritico::productoId).containsExactly(2L);
    }

    @Test
    void ultimosEventosSinDuplicadosMasRecientePrimeroYMaximo15() {
        for (int i = 0; i < 20; i++) {
            service.registrarPedido(creado("e" + i, (long) i, "local-01", "100", T0.plusSeconds(i), item(1L, "Pan", 1)));
        }
        service.registrarPedido(creado("e19", 19L, "local-01", "100", T0.plusSeconds(19), item(1L, "Pan", 1)));

        List<EventoReciente> ultimos = service.obtenerMetricas().ultimosEventos();
        assertThat(ultimos).hasSize(MetricasService.MAX_ULTIMOS_EVENTOS);
        assertThat(ultimos).extracting(EventoReciente::eventId).doesNotHaveDuplicates();
        assertThat(ultimos.getFirst().eventId()).isEqualTo("e19");
        assertThat(ultimos.getLast().eventId()).isEqualTo("e5");
        assertThat(service.obtenerMetricas().eventosProcesados()).isEqualTo(20);
    }

    @Test
    void datosIncoherentesLanzanEventoInvalido() {
        assertThatThrownBy(() -> service.registrarPedido(creado("e1", 9L, "local-01", "-1", T0)))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> service.registrarPedido(cambio("e2", 9L, null, "LISTO", T0)))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> service.registrarMovimiento(movimiento("m1", 1L, "STOCK_PERDIDO", 1, 1, T0)))
                .isInstanceOf(EventoInvalidoException.class);
        assertThat(service.obtenerMetricas().eventosProcesados()).isZero();
    }

    private static PedidoEvento creado(String eventId, Long pedidoId, String local, String total, Instant fecha,
                                       ItemPedidoEvento... items) {
        return new PedidoEvento(eventId, "PEDIDO_CREADO", fecha, pedidoId, local, "cliente-1", "RECIBIDO", null,
                "RETIRO_EN_TIENDA", new BigDecimal(total), List.of(items), "Cliente Uno", "CLIENTE");
    }

    private static PedidoEvento cambio(String eventId, Long pedidoId, String anterior, String nuevo, Instant fecha) {
        return new PedidoEvento(eventId, "ESTADO_CAMBIADO", fecha, pedidoId, "local-01", "cliente-1", nuevo, anterior,
                "RETIRO_EN_TIENDA", new BigDecimal("3000.00"), List.of(item(1L, "Pan amasado", 2)),
                "Operador", "OPERADOR_COCINA");
    }

    private static ItemPedidoEvento item(Long productoId, String nombre, int cantidad) {
        return new ItemPedidoEvento(productoId, nombre, cantidad, new BigDecimal("1500.00"));
    }

    private static MovimientoInventarioEvento movimiento(String eventId, Long productoId, String tipo, int cantidad,
                                                         int stock, Instant fecha) {
        return new MovimientoInventarioEvento(eventId, tipo, fecha, productoId, "local-01", "local-01-P" + productoId,
                "Producto " + productoId, cantidad, stock, "Admin", "ADMIN_GENERAL");
    }
}
