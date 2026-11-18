package cl.duocuc.pedidos360.auditoria.mensajeria.kafka.config;

import cl.duocuc.pedidos360.auditoria.auditoria.EventoInvalidoException;
import jakarta.validation.Validation;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.DeserializationException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Politica de reintentos y serializador del DLT, sin broker. */
class KafkaConsumerConfigTest {

    private final KafkaMensajeriaProperties props = new KafkaMensajeriaProperties(
            new KafkaMensajeriaProperties.Topicos("pedidos-eventos", "inventario-movimientos",
                    new KafkaMensajeriaProperties.Dlt(".DLT", 3, 3, 2, "delete", 1209600000L)),
            new KafkaMensajeriaProperties.Reintentos(3, 1000, 2.0, 10000));

    private final KafkaConsumerConfig config = new KafkaConsumerConfig(new KafkaProperties(), props,
            Validation.buildDefaultValidatorFactory().getValidator());

    private final ConsumerRecordRecoverer recoverer = mock(ConsumerRecordRecoverer.class);
    private final DefaultErrorHandler errorHandler = config.crearErrorHandler(recoverer);
    private final ConsumerRecord<String, Object> registro = new ConsumerRecord<>("pedidos-eventos", 1, 5L, "9", "x");

    @Test
    void eventoInvalidoVaDirectoAlDltSinReintentos() {
        boolean recuperado = manejar(new ListenerExecutionFailedException("fallo", new EventoInvalidoException("total negativo")));

        assertThat(recuperado).isTrue();
        verify(recoverer).accept(eq(registro), any());
    }

    @Test
    void errorDeDeserializacionVaDirectoAlDlt() {
        boolean recuperado = manejar(new DeserializationException("json ilegible",
                "no json".getBytes(StandardCharsets.UTF_8), false, new IllegalArgumentException()));

        assertThat(recuperado).isTrue();
        verify(recoverer).accept(eq(registro), any());
    }

    @Test
    void errorTransitorioSeReintentaAntesDeIrAlDlt() {
        boolean recuperado = manejar(new ListenerExecutionFailedException("fallo", new IllegalStateException("caida temporal")));

        assertThat(recuperado).isFalse();
        verify(recoverer, never()).accept(any(), any());
    }

    @Test
    void dltSeLlamaComoElTopicoMasElSufijo() {
        assertThat(props.dlt("pedidos-eventos")).isEqualTo("pedidos-eventos.DLT");
    }

    @Test
    void serializadorDelDltPublicaBytesOriginalesYObjetosComoJson() {
        try (DelegatingByTypeSerializer serializador = KafkaConsumerConfig.serializadorDlt()) {
            byte[] crudo = "no json".getBytes(StandardCharsets.UTF_8);

            assertThat(serializador.serialize("pedidos-eventos.DLT", crudo)).isEqualTo(crudo);
            assertThat(new String(serializador.serialize("pedidos-eventos.DLT", Map.of("pedidoId", 9)), StandardCharsets.UTF_8))
                    .isEqualTo("{\"pedidoId\":9}");
        }
    }

    private boolean manejar(Exception ex) {
        return errorHandler.handleOne(ex, registro, mock(Consumer.class), mock(MessageListenerContainer.class));
    }
}
