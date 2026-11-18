package cl.duocuc.pedidos360.reportes.metricas;

import cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento.ItemPedidoEvento;
import cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento.MovimientoInventarioEvento;
import cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento.PedidoEvento;
import cl.duocuc.pedidos360.reportes.metricas.MetricasResponse.EventoReciente;
import cl.duocuc.pedidos360.reportes.metricas.MetricasResponse.ProductoDemandado;
import cl.duocuc.pedidos360.reportes.metricas.MetricasResponse.StockCritico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Metricas del panel de operaciones (logica de negocio, sin Kafka).
 *
 * Idempotente: guarda el ULTIMO estado de cada pedido (por pedidoId) y el
 * ULTIMO stock de cada producto (por productoId), y calcula todo desde ahi.
 * Un evento repetido (mismo eventId) se ignora, asi releer el topico desde el
 * inicio o recibir un reintento no duplica ventas ni contadores.
 * Es seguro con varios hilos: lo llaman los 3 consumidores de cada topico.
 */
@Service
public class MetricasService {

    static final int MAX_ULTIMOS_EVENTOS = 15;
    static final int MAX_TOP_PRODUCTOS = 5;

    private static final Logger log = LoggerFactory.getLogger(MetricasService.class);

    private static final String PEDIDO_CREADO = "PEDIDO_CREADO";
    private static final String ESTADO_CAMBIADO = "ESTADO_CAMBIADO";
    private static final Set<String> TIPOS_PEDIDO = Set.of(PEDIDO_CREADO, ESTADO_CAMBIADO);
    private static final Set<String> TIPOS_INVENTARIO = Set.of("PRODUCTO_CREADO", "STOCK_DESCONTADO", "STOCK_REPUESTO");
    private static final String CANCELADO = "CANCELADO";

    private final int umbralStockCritico;
    private final Map<Long, PedidoEvento> pedidos = new ConcurrentHashMap<>();
    private final Map<Long, MovimientoInventarioEvento> stock = new ConcurrentHashMap<>();
    private final Set<String> eventosAplicados = ConcurrentHashMap.newKeySet();
    private final List<EventoReciente> ultimosEventos = new ArrayList<>();

    public MetricasService(@Value("${reportes.umbral-stock-critico:5}") int umbralStockCritico) {
        this.umbralStockCritico = umbralStockCritico;
    }

    /** Aplica un evento de pedido. Devuelve false si ya se habia aplicado (duplicado). */
    public boolean registrarPedido(PedidoEvento evento) {
        validarPedido(evento);
        if (!eventosAplicados.add(evento.eventId())) {
            log.debug("Evento {} ya aplicado: se ignora", evento.eventId());
            return false;
        }
        pedidos.merge(evento.pedidoId(), evento, (actual, nuevo) -> nuevo.fecha().isBefore(actual.fecha()) ? actual : nuevo);
        String descripcion = ESTADO_CAMBIADO.equals(evento.tipo())
                ? "Pedido #%d: %s -> %s".formatted(evento.pedidoId(), evento.estadoAnterior(), evento.estado())
                : "Pedido #%d creado por $%s".formatted(evento.pedidoId(), evento.total().toPlainString());
        agregarReciente(new EventoReciente(evento.eventId(), evento.tipo(), evento.fecha(), evento.localId(),
                descripcion, evento.usuario(), evento.rol()));
        return true;
    }

    /** Aplica un movimiento de inventario. Devuelve false si ya se habia aplicado (duplicado). */
    public boolean registrarMovimiento(MovimientoInventarioEvento evento) {
        validarMovimiento(evento);
        if (!eventosAplicados.add(evento.eventId())) {
            log.debug("Evento {} ya aplicado: se ignora", evento.eventId());
            return false;
        }
        stock.merge(evento.productoId(), evento, (actual, nuevo) -> nuevo.fecha().isBefore(actual.fecha()) ? actual : nuevo);
        String descripcion = "%s (#%d): %s %d, stock %d".formatted(evento.nombre(), evento.productoId(),
                evento.tipo(), evento.cantidad(), evento.stockResultante());
        agregarReciente(new EventoReciente(evento.eventId(), evento.tipo(), evento.fecha(), evento.localId(),
                descripcion, evento.usuario(), evento.rol()));
        return true;
    }

    public MetricasResponse obtenerMetricas() {
        List<PedidoEvento> actuales = List.copyOf(pedidos.values());
        List<PedidoEvento> vigentes = actuales.stream().filter(p -> !CANCELADO.equals(p.estado())).toList();

        BigDecimal ventasTotales = vigentes.stream().map(PedidoEvento::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Long> pedidosPorEstado = actuales.stream()
                .collect(Collectors.groupingBy(PedidoEvento::estado, TreeMap::new, Collectors.counting()));
        Map<String, BigDecimal> ventasPorLocal = vigentes.stream()
                .collect(Collectors.groupingBy(PedidoEvento::localId, TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, PedidoEvento::total, BigDecimal::add)));

        List<EventoReciente> recientes;
        synchronized (ultimosEventos) {
            recientes = List.copyOf(ultimosEventos);
        }
        return new MetricasResponse(actuales.size(), ventasTotales, pedidosPorEstado, ventasPorLocal,
                topProductos(vigentes), stockCritico(), recientes, eventosAplicados.size(), Instant.now());
    }

    private List<ProductoDemandado> topProductos(List<PedidoEvento> vigentes) {
        Map<Long, Long> unidades = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();
        for (PedidoEvento pedido : vigentes) {
            for (ItemPedidoEvento item : pedido.itemsOVacio()) {
                unidades.merge(item.productoId(), item.cantidad().longValue(), Long::sum);
                if (item.nombreProducto() != null) {
                    nombres.put(item.productoId(), item.nombreProducto());
                }
            }
        }
        return unidades.entrySet().stream()
                .map(e -> new ProductoDemandado(e.getKey(), nombres.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingLong(ProductoDemandado::unidades).reversed()
                        .thenComparing(ProductoDemandado::productoId))
                .limit(MAX_TOP_PRODUCTOS)
                .toList();
    }

    private List<StockCritico> stockCritico() {
        return stock.values().stream()
                .filter(m -> m.stockResultante() <= umbralStockCritico)
                .map(m -> new StockCritico(m.productoId(), m.localId(), m.sku(), m.nombre(), m.stockResultante()))
                .sorted(Comparator.comparingInt(StockCritico::stock).thenComparing(StockCritico::productoId))
                .toList();
    }

    /** Mas reciente primero (por fecha del evento), sin repetir eventId, maximo 15. */
    private void agregarReciente(EventoReciente nuevo) {
        synchronized (ultimosEventos) {
            ultimosEventos.removeIf(e -> e.eventId().equals(nuevo.eventId()));
            ultimosEventos.add(nuevo);
            ultimosEventos.sort(Comparator.comparing(EventoReciente::fecha).reversed());
            while (ultimosEventos.size() > MAX_ULTIMOS_EVENTOS) {
                ultimosEventos.removeLast();
            }
        }
    }

    // ---- Coherencia de negocio (ademas de @Valid en el listener) ----

    private void validarPedido(PedidoEvento e) {
        if (e == null || e.eventId() == null || e.pedidoId() == null || e.fecha() == null
                || e.estado() == null || e.localId() == null) {
            throw new EventoInvalidoException("Evento de pedido incompleto: " + e);
        }
        if (!TIPOS_PEDIDO.contains(e.tipo())) {
            throw new EventoInvalidoException("Tipo de evento de pedido desconocido: " + e.tipo());
        }
        if (e.total() == null || e.total().signum() < 0) {
            throw new EventoInvalidoException("Pedido #" + e.pedidoId() + " con total negativo o ausente: " + e.total());
        }
        if (ESTADO_CAMBIADO.equals(e.tipo()) && (e.estadoAnterior() == null || e.estadoAnterior().isBlank())) {
            throw new EventoInvalidoException("Pedido #" + e.pedidoId() + ": ESTADO_CAMBIADO sin estadoAnterior");
        }
        for (ItemPedidoEvento item : e.itemsOVacio()) {
            if (item == null || item.productoId() == null || item.cantidad() == null || item.cantidad() <= 0) {
                throw new EventoInvalidoException("Pedido #" + e.pedidoId() + " con un item invalido: " + item);
            }
        }
    }

    private void validarMovimiento(MovimientoInventarioEvento e) {
        if (e == null || e.eventId() == null || e.productoId() == null || e.fecha() == null) {
            throw new EventoInvalidoException("Movimiento de inventario incompleto: " + e);
        }
        if (!TIPOS_INVENTARIO.contains(e.tipo())) {
            throw new EventoInvalidoException("Tipo de movimiento de inventario desconocido: " + e.tipo());
        }
        if (e.cantidad() == null || e.cantidad() < 0 || e.stockResultante() == null || e.stockResultante() < 0) {
            throw new EventoInvalidoException("Producto #" + e.productoId() + " con cantidad o stock negativo");
        }
    }
}
