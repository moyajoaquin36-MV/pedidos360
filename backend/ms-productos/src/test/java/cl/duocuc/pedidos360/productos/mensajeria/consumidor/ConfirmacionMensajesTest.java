package cl.duocuc.pedidos360.productos.mensajeria.consumidor;

import cl.duocuc.pedidos360.productos.mensajeria.evento.StockBajoEvento;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ConfirmacionMensajesTest {

    private static final String JSON_VALIDO = """
            {"productoId":1,"nombre":"Pan","localId":"local-01","stock":2}""";

    private final ConfirmacionMensajes confirmacion = new ConfirmacionMensajes(JsonMapper.builder().build());
    private final Channel canal = mock(Channel.class);

    @Test
    void procesadoBienHaceAck() throws Exception {
        confirmacion.procesar(mensaje(JSON_VALIDO, false), canal, StockBajoEvento.class, evento -> { });
        verify(canal).basicAck(7L, false);
        verifyNoMoreInteractions(canal);
    }

    @Test
    void jsonInvalidoVaDirectoADlq() throws Exception {
        confirmacion.procesar(mensaje("esto no es json", false), canal, StockBajoEvento.class, evento -> { });
        verify(canal).basicNack(7L, false, false);
        verifyNoMoreInteractions(canal);
    }

    @Test
    void mensajeInvalidoVaDirectoADlq() throws Exception {
        confirmacion.procesar(mensaje(JSON_VALIDO, false), canal, StockBajoEvento.class, evento -> {
            throw new MensajeInvalidoException("faltan datos");
        });
        verify(canal).basicNack(7L, false, false);
        verifyNoMoreInteractions(canal);
    }

    @Test
    void errorEnPrimeraEntregaSeReintenta() throws Exception {
        confirmacion.procesar(mensaje(JSON_VALIDO, false), canal, StockBajoEvento.class, evento -> {
            throw new IllegalStateException("caida temporal");
        });
        verify(canal).basicNack(7L, false, true);
        verifyNoMoreInteractions(canal);
    }

    @Test
    void errorTrasReintentoVaADlq() throws Exception {
        confirmacion.procesar(mensaje(JSON_VALIDO, true), canal, StockBajoEvento.class, evento -> {
            throw new IllegalStateException("caida temporal");
        });
        verify(canal).basicNack(7L, false, false);
        verifyNoMoreInteractions(canal);
    }

    private Message mensaje(String cuerpo, boolean reentregado) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setDeliveryTag(7L);
        propiedades.setRedelivered(reentregado);
        propiedades.setConsumerQueue("inventario.alertas");
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), propiedades);
    }
}
