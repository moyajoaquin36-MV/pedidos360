package cl.duocuc.pedidos360.auditoria.mensajeria.kafka.consumidor;

import cl.duocuc.pedidos360.auditoria.auditoria.AuditoriaService;
import cl.duocuc.pedidos360.auditoria.auditoria.OrigenEvento;
import cl.duocuc.pedidos360.auditoria.mensajeria.kafka.config.KafkaConsumerConfig;
import cl.duocuc.pedidos360.auditoria.mensajeria.kafka.evento.PedidoEvento;
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
 * Consumidor de pedidos-eventos (grupo-auditoria). Solo es infraestructura:
 * valida el payload, deja el contexto en el log y delega en AuditoriaService.
 * A diferencia de ms-reportes no rebobina: la auditoria ya esta en MySQL y
 * continua desde su ultimo offset confirmado.
 */
@Component
public class PedidosEventosListener {

    private static final Logger log = LoggerFactory.getLogger(PedidosEventosListener.class);

    private final AuditoriaService auditoriaService;

    public PedidosEventosListener(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @KafkaListener(id = "auditoria-pedidos-eventos",
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
            log.info("Evento recibido topico={} particion={} offset={} clave={} tipo={} pedidoId={} usuario={} rol={}",
                    topico, particion, offset, clave, evento.tipo(), evento.pedidoId(), evento.usuario(), evento.rol());
            auditoriaService.registrarPedido(evento, new OrigenEvento(topico, particion, offset, clave));
        } finally {
            MDC.remove(Correlacion.MDC_CLAVE);
        }
    }
}
