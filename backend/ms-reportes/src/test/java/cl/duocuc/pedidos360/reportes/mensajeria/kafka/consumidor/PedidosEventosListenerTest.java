package cl.duocuc.pedidos360.reportes.mensajeria.kafka.consumidor;

import cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento.ItemPedidoEvento;
import cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento.PedidoEvento;
import cl.duocuc.pedidos360.reportes.metricas.MetricasService;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PedidosEventosListenerTest {

    private final MetricasService metricasService = mock(MetricasService.class);
    private final PedidosEventosListener listener = new PedidosEventosListener(metricasService);

    @Test
    void delegaElEventoAlServicioConElCorrelationIdEnElMdc() {
        PedidoEvento evento = new PedidoEvento("evt-1", "PEDIDO_CREADO", Instant.parse("2026-11-17T22:30:00Z"), 9L,
                "local-01", "cliente-1", "RECIBIDO", null, "RETIRO_EN_TIENDA", new BigDecimal("3000.00"),
                List.of(new ItemPedidoEvento(1L, "Pan amasado", 2, new BigDecimal("1500.00"))), "Cliente", "CLIENTE");
        AtomicReference<String> mdcDuranteProceso = new AtomicReference<>();
        doAnswer(inv -> {
            mdcDuranteProceso.set(MDC.get(Correlacion.MDC_CLAVE));
            return true;
        }).when(metricasService).registrarPedido(any());

        listener.recibir(evento, "pedidos-eventos", 1, 42L, "9", "corr-123");

        verify(metricasService).registrarPedido(evento);
        assertThat(mdcDuranteProceso.get()).isEqualTo("corr-123");
        assertThat(MDC.get(Correlacion.MDC_CLAVE)).isNull();
    }

    @Test
    void sinHeaderUsaElEventIdComoCorrelationId() {
        assertThat(Correlacion.resolver(null, "evt-1")).isEqualTo("evt-1");
        assertThat(Correlacion.resolver("corr-9", "evt-1")).isEqualTo("corr-9");
    }

    @Test
    void alAsignarParticionesRebobinaAlInicio() {
        ConsumerSeekCallback callback = mock(ConsumerSeekCallback.class);
        Map<TopicPartition, Long> asignadas = Map.of(new TopicPartition("pedidos-eventos", 0), 10L,
                new TopicPartition("pedidos-eventos", 1), 7L);

        listener.onPartitionsAssigned(asignadas, callback);

        verify(callback).seekToBeginning(asignadas.keySet());
    }
}
