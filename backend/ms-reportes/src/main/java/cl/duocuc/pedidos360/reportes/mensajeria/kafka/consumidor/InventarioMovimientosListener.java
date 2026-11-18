package cl.duocuc.pedidos360.reportes.mensajeria.kafka.consumidor;

import cl.duocuc.pedidos360.reportes.mensajeria.kafka.config.KafkaConsumerConfig;
import cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento.MovimientoInventarioEvento;
import cl.duocuc.pedidos360.reportes.metricas.MetricasService;
import jakarta.validation.Valid;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumidor de inventario-movimientos (grupo-reporteria): alimenta el stock
 * critico del panel. Igual que el de pedidos, relee desde el inicio al
 * asignarse particiones (el topico esta compactado: queda el ultimo stock por
 * producto, que es justo lo que se necesita).
 */
@Component
public class InventarioMovimientosListener extends AbstractConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(InventarioMovimientosListener.class);

    private final MetricasService metricasService;

    public InventarioMovimientosListener(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @KafkaListener(id = "reportes-inventario-movimientos",
            topics = "${kafka.topicos.inventario-movimientos}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = KafkaConsumerConfig.FACTORY_INVENTARIO)
    public void recibir(@Valid @Payload MovimientoInventarioEvento evento,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topico,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String clave,
                        @Header(name = Correlacion.HEADER, required = false) String correlationId) {
        MDC.put(Correlacion.MDC_CLAVE, Correlacion.resolver(correlationId, evento.eventId()));
        try {
            log.info("Evento recibido topico={} particion={} offset={} clave={} tipo={} productoId={} stock={} local={} rol={}",
                    topico, particion, offset, clave, evento.tipo(), evento.productoId(), evento.stockResultante(),
                    evento.localId(), evento.rol());
            metricasService.registrarMovimiento(evento);
        } finally {
            MDC.remove(Correlacion.MDC_CLAVE);
        }
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        super.onPartitionsAssigned(assignments, callback);
        log.info("Particiones asignadas {}: se releen desde el inicio para reconstruir el stock", assignments.keySet());
        callback.seekToBeginning(assignments.keySet());
    }
}
