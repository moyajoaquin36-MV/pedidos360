package cl.duocuc.pedidos360.productos.mensajeria.consumidor;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * ACK manual comun a todos los consumidores (acknowledge mode MANUAL).
 *
 * Decision por cada mensaje:
 *  - Procesado bien                       -> basicAck.
 *  - JSON invalido o datos incompletos    -> basicNack sin requeue -> DLQ (no tiene arreglo).
 *  - Otro error, primera entrega          -> basicNack con requeue (un reintento).
 *  - Otro error, ya reintentado           -> basicNack sin requeue -> DLQ.
 * Todo envio a DLQ queda registrado en el log con el contenido del mensaje.
 */
@Component
public class ConfirmacionMensajes {

    private static final Logger log = LoggerFactory.getLogger(ConfirmacionMensajes.class);

    private final JsonMapper jsonMapper;

    public ConfirmacionMensajes(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public <T> void procesar(Message mensaje, Channel canal, Class<T> tipo, Consumer<T> logica) throws IOException {
        MessageProperties propiedades = mensaje.getMessageProperties();
        long tag = propiedades.getDeliveryTag();
        String cola = propiedades.getConsumerQueue();
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);

        try {
            logica.accept(jsonMapper.readValue(mensaje.getBody(), tipo));
            canal.basicAck(tag, false);
        } catch (JacksonException | MensajeInvalidoException e) {
            log.error("[{}] Mensaje invalido, enviado a DLQ: {} | cuerpo={}", cola, e.getMessage(), cuerpo);
            canal.basicNack(tag, false, false);
        } catch (RuntimeException e) {
            if (Boolean.TRUE.equals(propiedades.getRedelivered())) {
                log.error("[{}] Fallo de nuevo tras reintentar, enviado a DLQ: {} | cuerpo={}", cola, e.getMessage(), cuerpo);
                canal.basicNack(tag, false, false);
            } else {
                log.warn("[{}] Error al procesar, se reintenta una vez: {}", cola, e.getMessage());
                canal.basicNack(tag, false, true);
            }
        }
    }
}
