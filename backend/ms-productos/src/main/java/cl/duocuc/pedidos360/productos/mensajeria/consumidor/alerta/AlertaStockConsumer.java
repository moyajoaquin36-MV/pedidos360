package cl.duocuc.pedidos360.productos.mensajeria.consumidor.alerta;

import cl.duocuc.pedidos360.productos.mensajeria.consumidor.ConfirmacionMensajes;
import cl.duocuc.pedidos360.productos.mensajeria.consumidor.MensajeInvalidoException;
import cl.duocuc.pedidos360.productos.mensajeria.evento.StockBajoEvento;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Dominio alerta de inventario: avisa al local que debe reponer un producto. */
@Component
public class AlertaStockConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertaStockConsumer.class);

    private final ConfirmacionMensajes confirmacion;

    public AlertaStockConsumer(ConfirmacionMensajes confirmacion) {
        this.confirmacion = confirmacion;
    }

    @RabbitListener(queues = "${mensajeria.colas.alertas-stock}", ackMode = "MANUAL")
    public void recibir(Message mensaje, Channel canal) throws IOException {
        confirmacion.procesar(mensaje, canal, StockBajoEvento.class, this::alertar);
    }

    private void alertar(StockBajoEvento evento) {
        if (evento.productoId() == null || evento.stock() == null) {
            throw new MensajeInvalidoException("Evento sin productoId o stock");
        }
        log.warn("ALERTA STOCK BAJO en {}: '{}' (#{}) quedo con {} unidad(es), hay que reponer",
                evento.localId(), evento.nombre(), evento.productoId(), evento.stock());
    }
}
