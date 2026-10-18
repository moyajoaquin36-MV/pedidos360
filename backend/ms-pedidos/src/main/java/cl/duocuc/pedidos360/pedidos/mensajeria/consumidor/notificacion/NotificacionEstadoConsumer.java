package cl.duocuc.pedidos360.pedidos.mensajeria.consumidor.notificacion;

import cl.duocuc.pedidos360.pedidos.mensajeria.consumidor.ConfirmacionMensajes;
import cl.duocuc.pedidos360.pedidos.mensajeria.consumidor.MensajeInvalidoException;
import cl.duocuc.pedidos360.pedidos.mensajeria.evento.EstadoPedidoEvento;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Dominio notificacion: avisa al cliente cada cambio de estado de su pedido. */
@Component
public class NotificacionEstadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacionEstadoConsumer.class);

    private final ConfirmacionMensajes confirmacion;

    public NotificacionEstadoConsumer(ConfirmacionMensajes confirmacion) {
        this.confirmacion = confirmacion;
    }

    @RabbitListener(queues = "${mensajeria.colas.notificaciones}", ackMode = "MANUAL")
    public void recibir(Message mensaje, Channel canal) throws IOException {
        confirmacion.procesar(mensaje, canal, EstadoPedidoEvento.class, this::notificar);
    }

    private void notificar(EstadoPedidoEvento evento) {
        if (evento.pedidoId() == null || evento.clienteId() == null || evento.estado() == null) {
            throw new MensajeInvalidoException("Evento sin pedidoId, clienteId o estado");
        }
        log.info("NOTIFICACION para cliente {}: tu pedido #{} ahora esta {}",
                evento.clienteId(), evento.pedidoId(), evento.estado());
    }
}
