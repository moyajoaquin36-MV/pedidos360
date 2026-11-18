package cl.duocuc.pedidos360.auditoria.mensajeria.kafka.consumidor;

import cl.duocuc.pedidos360.auditoria.auditoria.AuditoriaService;
import cl.duocuc.pedidos360.auditoria.auditoria.OrigenEvento;
import cl.duocuc.pedidos360.auditoria.mensajeria.kafka.config.KafkaConsumerConfig;
import cl.duocuc.pedidos360.auditoria.mensajeria.kafka.evento.MovimientoInventarioEvento;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor de inventario-movimientos (grupo-auditoria): guarda cada
 * movimiento de stock con quien lo hizo. Continua desde su ultimo offset.
 */
@Component
public class InventarioMovimientosListener {

    private static final Logger log = LoggerFactory.getLogger(InventarioMovimientosListener.class);

    private final AuditoriaService auditoriaService;

    public InventarioMovimientosListener(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @KafkaListener(id = "auditoria-inventario-movimientos",
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
            log.info("Evento recibido topico={} particion={} offset={} clave={} tipo={} productoId={} usuario={} rol={}",
                    topico, particion, offset, clave, evento.tipo(), evento.productoId(), evento.usuario(), evento.rol());
            auditoriaService.registrarMovimiento(evento, new OrigenEvento(topico, particion, offset, clave));
        } finally {
            MDC.remove(Correlacion.MDC_CLAVE);
        }
    }
}
