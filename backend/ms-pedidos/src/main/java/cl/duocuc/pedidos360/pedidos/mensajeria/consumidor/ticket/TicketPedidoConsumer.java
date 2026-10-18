package cl.duocuc.pedidos360.pedidos.mensajeria.consumidor.ticket;

import cl.duocuc.pedidos360.pedidos.mensajeria.consumidor.ConfirmacionMensajes;
import cl.duocuc.pedidos360.pedidos.mensajeria.consumidor.MensajeInvalidoException;
import cl.duocuc.pedidos360.pedidos.mensajeria.evento.PedidoCreadoEvento;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Dominio ticket: genera el ticket (comprobante) de cada pedido creado. */
@Component
public class TicketPedidoConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketPedidoConsumer.class);

    private final ConfirmacionMensajes confirmacion;

    public TicketPedidoConsumer(ConfirmacionMensajes confirmacion) {
        this.confirmacion = confirmacion;
    }

    @RabbitListener(queues = "${mensajeria.colas.tickets}", ackMode = "MANUAL")
    public void recibir(Message mensaje, Channel canal) throws IOException {
        confirmacion.procesar(mensaje, canal, PedidoCreadoEvento.class, this::generarTicket);
    }

    private void generarTicket(PedidoCreadoEvento evento) {
        if (evento.pedidoId() == null || evento.total() == null) {
            throw new MensajeInvalidoException("Evento sin pedidoId o total");
        }
        log.info("TICKET Pedido #{} | local {} | {} | {} item(s) | total ${} | {}",
                evento.pedidoId(), evento.localId(), evento.modalidadEntrega(),
                evento.cantidadItems(), evento.total(), evento.fecha());
    }
}
