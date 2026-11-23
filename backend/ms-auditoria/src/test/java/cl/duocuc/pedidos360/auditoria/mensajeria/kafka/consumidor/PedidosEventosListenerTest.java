package cl.duocuc.pedidos360.auditoria.mensajeria.kafka.consumidor;

import cl.duocuc.pedidos360.auditoria.auditoria.AuditoriaService;
import cl.duocuc.pedidos360.auditoria.auditoria.OrigenEvento;
import cl.duocuc.pedidos360.auditoria.mensajeria.kafka.evento.PedidoEvento;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PedidosEventosListenerTest {

    private final AuditoriaService auditoriaService = mock(AuditoriaService.class);
    private final PedidosEventosListener listener = new PedidosEventosListener(auditoriaService);

    @Test
    void delegaElEventoConSuOrigenYLimpiaElMdc() {
        PedidoEvento evento = new PedidoEvento("evt-1", "ESTADO_CAMBIADO", Instant.parse("2026-11-17T22:30:00Z"), 9L,
                "local-01", "sub-1", "LISTO", "EN_PREPARACION", "DELIVERY", new BigDecimal("3000.00"), List.of(),
                "Operador", "OPERADOR_COCINA");

        listener.recibir(evento, "pedidos-eventos", 2, 15L, "9", null);

        verify(auditoriaService).registrarPedido(evento, new OrigenEvento("pedidos-eventos", 2, 15L, "9"));
        assertThat(MDC.get(Correlacion.MDC_CLAVE)).isNull();
    }
}
