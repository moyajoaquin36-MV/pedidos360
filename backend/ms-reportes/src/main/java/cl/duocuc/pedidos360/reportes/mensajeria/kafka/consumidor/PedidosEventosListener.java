package cl.duocuc.pedidos360.reportes.mensajeria.kafka.consumidor;

import cl.duocuc.pedidos360.reportes.mensajeria.kafka.config.KafkaConsumerConfig;
import cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento.PedidoEvento;
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
 * Consumidor de pedidos-eventos (grupo-reporteria). Solo es infraestructura:
 * valida el payload, deja el contexto en el log y delega en MetricasService.
 *
 * Reconstruccion al arrancar: las metricas viven en memoria, asi que al
 * asignarse particiones se relee el topico desde el inicio. Es seguro porque
 * MetricasService es idempotente (ultimo estado por pedidoId, eventId unico).
 */
@Component
public class PedidosEventosListener extends AbstractConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(PedidosEventosListener.class);

    private final MetricasService metricasService;

    public PedidosEventosListener(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @KafkaListener(id = "reportes-pedidos-eventos",
            topics = "${kafka.topicos.pedidos-eventos}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = KafkaConsumerConfig.FACTORY_PEDIDOS)
    public void recibir(@Valid @Payload PedidoEvento evento,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topico,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String clave,
                        @Header(name = Correlacion.HEADER, required = false) String correlationId) {
        MDC.put(Correlacion.MDC_CLAVE, Correlacion.resolver(correlationId, evento.eventId()));
        try {
            log.info("Evento recibido topico={} particion={} offset={} clave={} tipo={} pedidoId={} estado={} local={} rol={}",
                    topico, particion, offset, clave, evento.tipo(), evento.pedidoId(), evento.estado(),
                    evento.localId(), evento.rol());
            metricasService.registrarPedido(evento);
        } finally {
            MDC.remove(Correlacion.MDC_CLAVE);
        }
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        super.onPartitionsAssigned(assignments, callback);
        log.info("Particiones asignadas {}: se releen desde el inicio para reconstruir las metricas", assignments.keySet());
        callback.seekToBeginning(assignments.keySet());
    }
}
