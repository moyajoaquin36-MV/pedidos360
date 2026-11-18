package cl.duocuc.pedidos360.auditoria.mensajeria.kafka.consumidor;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;

/**
 * Header correlation-id del contrato (igual al eventId) y su clave en el MDC,
 * para que todas las lineas de log de un evento se puedan seguir juntas.
 */
public final class Correlacion {

    public static final String HEADER = "correlation-id";
    public static final String MDC_CLAVE = "correlationId";

    private Correlacion() {
    }

    /** Usa el header; si el productor no lo envio, el eventId (el contrato los hace iguales). */
    public static String resolver(String header, String eventId) {
        return header != null && !header.isBlank() ? header : eventId;
    }

    /** Lee el header desde el registro crudo (logs del error handler, fuera del listener). */
    public static String desde(ConsumerRecord<?, ?> registro) {
        Header header = registro.headers().lastHeader(HEADER);
        return header == null || header.value() == null ? "-" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
