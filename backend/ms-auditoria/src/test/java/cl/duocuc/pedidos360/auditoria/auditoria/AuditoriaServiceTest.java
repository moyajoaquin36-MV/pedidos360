package cl.duocuc.pedidos360.auditoria.auditoria;

import cl.duocuc.pedidos360.auditoria.domain.EventoAuditoria;
import cl.duocuc.pedidos360.auditoria.domain.TipoEntidad;
import cl.duocuc.pedidos360.auditoria.mensajeria.kafka.evento.ItemPedidoEvento;
import cl.duocuc.pedidos360.auditoria.mensajeria.kafka.evento.MovimientoInventarioEvento;
import cl.duocuc.pedidos360.auditoria.mensajeria.kafka.evento.PedidoEvento;
import cl.duocuc.pedidos360.auditoria.repository.EventoAuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Contra H2 real: restriccion UNIQUE de eventId y consultas ordenadas. */
@SpringBootTest
class AuditoriaServiceTest {

    private static final Instant T0 = Instant.parse("2026-11-17T22:30:00Z");
    private static final OrigenEvento ORIGEN_PEDIDOS = new OrigenEvento("pedidos-eventos", 1, 42L, "9");

    @Autowired
    private AuditoriaService service;

    @Autowired
    private EventoAuditoriaRepository repository;

    @BeforeEach
    void limpiar() {
        repository.deleteAll();
    }

    @Test
    void guardaElEventoConUsuarioRolYOrigen() {
        assertThat(service.registrarPedido(pedido("e1", 9L, "PEDIDO_CREADO", T0), ORIGEN_PEDIDOS)).isTrue();

        EventoAuditoria guardado = repository.findAll().getFirst();
        assertThat(guardado.getEntidad()).isEqualTo(TipoEntidad.PEDIDO);
        assertThat(guardado.getEntidadId()).isEqualTo(9L);
        assertThat(guardado.getUsuario()).isEqualTo("Joaquin Moya");
        assertThat(guardado.getRol()).isEqualTo("CLIENTE");
        assertThat(guardado.getFechaEvento()).isEqualTo(T0);
        assertThat(guardado.getRecibidoEn()).isNotNull();
        assertThat(guardado.getTopico()).isEqualTo("pedidos-eventos");
        assertThat(guardado.getOffset()).isEqualTo(42L);
        assertThat(guardado.getPayload()).contains("\"pedidoId\":9").contains("\"eventId\":\"e1\"");
    }

    @Test
    void eventoDuplicadoSeDescartaSinFallar() {
        PedidoEvento evento = pedido("e1", 9L, "PEDIDO_CREADO", T0);

        assertThat(service.registrarPedido(evento, ORIGEN_PEDIDOS)).isTrue();
        assertThat(service.registrarPedido(evento, new OrigenEvento("pedidos-eventos", 1, 43L, "9"))).isFalse();

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void tipoQueNoCorrespondeAlTopicoEsInvalido() {
        assertThatThrownBy(() -> service.registrarPedido(pedido("e1", 9L, "STOCK_DESCONTADO", T0), ORIGEN_PEDIDOS))
                .isInstanceOf(EventoInvalidoException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void buscaPorEntidadYTipoMasRecientePrimeroConLimite() {
        service.registrarPedido(pedido("e1", 9L, "PEDIDO_CREADO", T0), ORIGEN_PEDIDOS);
        service.registrarPedido(pedido("e2", 9L, "ESTADO_CAMBIADO", T0.plusSeconds(60)), ORIGEN_PEDIDOS);
        service.registrarPedido(pedido("e3", 9L, "ESTADO_CAMBIADO", T0.plusSeconds(120)), ORIGEN_PEDIDOS);
        service.registrarPedido(pedido("e4", 10L, "PEDIDO_CREADO", T0), ORIGEN_PEDIDOS);
        service.registrarMovimiento(movimiento("m1", 9L), new OrigenEvento("inventario-movimientos", 0, 1L, "9"));

        assertThat(ids(service.buscar(9L, null, null, 50))).containsExactly("e3", "e2", "e1");
        assertThat(ids(service.buscar(9L, null, null, 2))).containsExactly("e3", "e2");
        assertThat(ids(service.buscar(9L, null, "ESTADO_CAMBIADO", 50))).containsExactly("e3", "e2");
        assertThat(ids(service.buscar(null, 9L, null, 50))).containsExactly("m1");
        assertThat(ids(service.buscar(null, null, "PEDIDO_CREADO", 50))).containsExactlyInAnyOrder("e1", "e4");
        assertThat(service.buscar(null, null, null, 50)).hasSize(5).first()
                .satisfies(e -> assertThat(e.getEventId()).isEqualTo("e3"));
    }

    @Test
    void noSePuedeFiltrarPorPedidoYProductoALaVez() {
        assertThatThrownBy(() -> service.buscar(1L, 2L, null, 50)).isInstanceOf(IllegalArgumentException.class);
    }

    private static List<String> ids(List<EventoAuditoria> eventos) {
        return eventos.stream().map(EventoAuditoria::getEventId).toList();
    }

    private static PedidoEvento pedido(String eventId, Long pedidoId, String tipo, Instant fecha) {
        return new PedidoEvento(eventId, tipo, fecha, pedidoId, "local-01", "sub-1", "RECIBIDO",
                "ESTADO_CAMBIADO".equals(tipo) ? "RECIBIDO" : null, "RETIRO_EN_TIENDA", new BigDecimal("3000.00"),
                List.of(new ItemPedidoEvento(1L, "Pan amasado", 2, new BigDecimal("1500.00"))), "Joaquin Moya", "CLIENTE");
    }

    private static MovimientoInventarioEvento movimiento(String eventId, Long productoId) {
        return new MovimientoInventarioEvento(eventId, "STOCK_DESCONTADO", T0, productoId, "local-01", "local-01-PAN",
                "Pan amasado", 2, 75, "Joaquin Moya", "CLIENTE");
    }
}
