package cl.duocuc.pedidos360.pedidos.mensajeria.kafka.productor;

import cl.duocuc.pedidos360.pedidos.domain.EstadoPedido;
import cl.duocuc.pedidos360.pedidos.domain.ItemPedido;
import cl.duocuc.pedidos360.pedidos.domain.ModalidadEntrega;
import cl.duocuc.pedidos360.pedidos.domain.Pedido;
import cl.duocuc.pedidos360.pedidos.mensajeria.kafka.config.KafkaTopicosProperties;
import cl.duocuc.pedidos360.pedidos.mensajeria.kafka.evento.Actor;
import cl.duocuc.pedidos360.pedidos.mensajeria.kafka.evento.PedidoEvento;
import cl.duocuc.pedidos360.pedidos.mensajeria.kafka.evento.TipoEventoPedido;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class PedidoKafkaPublisherTest {

    private static final String TOPICO = "pedidos-eventos";

    private final KafkaTemplate<String, PedidoEvento> kafkaTemplate = mock(KafkaTemplate.class);
    private final PedidoKafkaPublisher publisher = new PedidoKafkaPublisher(kafkaTemplate,
            new KafkaTopicosProperties(new KafkaTopicosProperties.Topico(TOPICO, 3, 3, Map.of())));
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void brokerConfirmaElEnvio() {
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenAnswer(invocacion -> {
            ProducerRecord<String, PedidoEvento> registro = invocacion.getArgument(0);
            RecordMetadata metadata = new RecordMetadata(new TopicPartition(registro.topic(), 1), 42L, 0, 0L, 0, 0);
            return CompletableFuture.completedFuture(new SendResult<>(registro, metadata));
        });
    }

    @AfterEach
    void limpiarSincronizacion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void pedidoCreadoVaAlTopicoConClavePedidoIdYCorrelationId() {
        publisher.pedidoCreado(pedido(9L, EstadoPedido.RECIBIDO), new Actor("Joaquin Moya", "CLIENTE"));

        ProducerRecord<String, PedidoEvento> registro = registroEnviado();
        PedidoEvento evento = registro.value();
        assertThat(registro.topic()).isEqualTo(TOPICO);
        assertThat(registro.key()).isEqualTo("9");
        assertThat(new String(registro.headers().lastHeader("correlation-id").value(), StandardCharsets.UTF_8))
                .isEqualTo(evento.eventId());

        assertThat(evento.tipo()).isEqualTo(TipoEventoPedido.PEDIDO_CREADO);
        assertThat(evento.eventId()).isNotBlank();
        assertThat(evento.fecha()).isNotNull();
        assertThat(evento.pedidoId()).isEqualTo(9L);
        assertThat(evento.localId()).isEqualTo("local-01");
        assertThat(evento.clienteId()).isEqualTo("sub-cliente");
        assertThat(evento.estado()).isEqualTo("RECIBIDO");
        assertThat(evento.estadoAnterior()).isNull();
        assertThat(evento.modalidadEntrega()).isEqualTo("RETIRO_EN_TIENDA");
        assertThat(evento.total()).isEqualByComparingTo("3000");
        assertThat(evento.items()).containsExactly(
                new PedidoEvento.Item(1L, "Pan amasado", 2, new BigDecimal("1500")));
        assertThat(evento.usuario()).isEqualTo("Joaquin Moya");
        assertThat(evento.rol()).isEqualTo("CLIENTE");
        assertThat(validator.validate(evento)).isEmpty();
    }

    @Test
    void estadoCambiadoLlevaElEstadoNuevoYElAnterior() {
        publisher.estadoCambiado(pedido(9L, EstadoPedido.LISTO), EstadoPedido.EN_PREPARACION,
                new Actor("Ana Operadora", "OPERADOR_COCINA"));

        ProducerRecord<String, PedidoEvento> registro = registroEnviado();
        PedidoEvento evento = registro.value();
        assertThat(registro.key()).isEqualTo("9");
        assertThat(evento.tipo()).isEqualTo(TipoEventoPedido.ESTADO_CAMBIADO);
        assertThat(evento.estado()).isEqualTo("LISTO");
        assertThat(evento.estadoAnterior()).isEqualTo("EN_PREPARACION");
        // El evento es autosuficiente: lleva items y total aunque solo cambie el estado.
        assertThat(evento.items()).hasSize(1);
        assertThat(evento.total()).isEqualByComparingTo("3000");
        assertThat(evento.usuario()).isEqualTo("Ana Operadora");
        assertThat(evento.rol()).isEqualTo("OPERADOR_COCINA");
        assertThat(validator.validate(evento)).isEmpty();
    }

    @Test
    void conTransaccionActivaPublicaSoloDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.pedidoCreado(pedido(9L, EstadoPedido.RECIBIDO), new Actor("Joaquin Moya", "CLIENTE"));
        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }

    @Test
    void siKafkaFallaAlEnviarNoRompeLaRequest() {
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenThrow(new KafkaException("broker caido"));

        assertThatCode(() -> publisher.pedidoCreado(pedido(9L, EstadoPedido.RECIBIDO), new Actor("u", "CLIENTE")))
                .doesNotThrowAnyException();
    }

    @Test
    void siElBrokerRechazaElEventoSoloSeRegistra() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("timeout")));

        assertThatCode(() -> publisher.pedidoCreado(pedido(9L, EstadoPedido.RECIBIDO), new Actor("u", "CLIENTE")))
                .doesNotThrowAnyException();
    }

    /** Con la configuracion de application.yml el JSON calza con el contrato y no lleva headers de tipo. */
    @Test
    void elSerializerEscribeFechaIsoYTotalNumericoSinHeadersDeTipo() {
        PedidoEvento evento = new PedidoEvento("e-1", TipoEventoPedido.PEDIDO_CREADO,
                Instant.parse("2026-11-17T22:30:00Z"), 9L, "local-01", "sub", "RECIBIDO", null,
                "RETIRO_EN_TIENDA", new BigDecimal("3000.00"),
                List.of(new PedidoEvento.Item(1L, "Pan amasado", 2, new BigDecimal("1500.00"))),
                "Joaquin Moya", "CLIENTE");

        try (JacksonJsonSerializer<PedidoEvento> serializer = new JacksonJsonSerializer<>()) {
            serializer.configure(Map.of("spring.json.add.type.headers", false), false);
            RecordHeaders headers = new RecordHeaders();
            String json = new String(serializer.serialize(TOPICO, headers, evento), StandardCharsets.UTF_8);

            assertThat(json)
                    .contains("\"tipo\":\"PEDIDO_CREADO\"")
                    .contains("\"fecha\":\"2026-11-17T22:30:00Z\"")
                    .contains("\"total\":3000.00")
                    .contains("\"precioUnitario\":1500.00")
                    .contains("\"estadoAnterior\":null");
            assertThat(headers.toArray()).isEmpty();
        }
    }

    private ProducerRecord<String, PedidoEvento> registroEnviado() {
        ArgumentCaptor<ProducerRecord<String, PedidoEvento>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        return captor.getValue();
    }

    private Pedido pedido(Long id, EstadoPedido estado) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setLocalId("local-01");
        pedido.setClienteId("sub-cliente");
        pedido.setModalidadEntrega(ModalidadEntrega.RETIRO_EN_TIENDA);
        pedido.setEstado(estado);

        ItemPedido item = new ItemPedido();
        item.setProductoId(1L);
        item.setNombreProducto("Pan amasado");
        item.setCantidad(2);
        item.setPrecioUnitario(new BigDecimal("1500"));
        pedido.agregarItem(item);
        return pedido;
    }
}
