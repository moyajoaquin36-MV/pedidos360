package cl.duocuc.pedidos360.productos.mensajeria.kafka.productor;

import cl.duocuc.pedidos360.productos.domain.Producto;
import cl.duocuc.pedidos360.productos.mensajeria.kafka.config.KafkaTopicosProperties;
import cl.duocuc.pedidos360.productos.mensajeria.kafka.evento.Actor;
import cl.duocuc.pedidos360.productos.mensajeria.kafka.evento.MovimientoInventarioEvento;
import cl.duocuc.pedidos360.productos.mensajeria.kafka.evento.TipoMovimiento;
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
class InventarioKafkaPublisherTest {

    private static final String TOPICO = "inventario-movimientos";

    private final KafkaTemplate<String, MovimientoInventarioEvento> kafkaTemplate = mock(KafkaTemplate.class);
    private final InventarioKafkaPublisher publisher = new InventarioKafkaPublisher(kafkaTemplate,
            new KafkaTopicosProperties(new KafkaTopicosProperties.Topico(TOPICO, 3, 3, Map.of())));
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final Actor cliente = new Actor("Joaquin Moya", "CLIENTE");

    @BeforeEach
    void brokerConfirmaElEnvio() {
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenAnswer(invocacion -> {
            ProducerRecord<String, MovimientoInventarioEvento> registro = invocacion.getArgument(0);
            RecordMetadata metadata = new RecordMetadata(new TopicPartition(registro.topic(), 2), 7L, 0, 0L, 0, 0);
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
    void stockDescontadoVaAlTopicoConClaveProductoIdYStockResultante() {
        Producto producto = producto(1L, 75);

        publisher.stockDescontado(producto, 2, cliente);

        ProducerRecord<String, MovimientoInventarioEvento> registro = registroEnviado();
        MovimientoInventarioEvento evento = registro.value();
        assertThat(registro.topic()).isEqualTo(TOPICO);
        assertThat(registro.key()).isEqualTo("1");
        assertThat(new String(registro.headers().lastHeader("correlation-id").value(), StandardCharsets.UTF_8))
                .isEqualTo(evento.eventId());

        assertThat(evento.tipo()).isEqualTo(TipoMovimiento.STOCK_DESCONTADO);
        assertThat(evento.eventId()).isNotBlank();
        assertThat(evento.fecha()).isNotNull();
        assertThat(evento.productoId()).isEqualTo(1L);
        assertThat(evento.localId()).isEqualTo("local-01");
        assertThat(evento.sku()).isEqualTo("local-01-PAN");
        assertThat(evento.nombre()).isEqualTo("Pan amasado");
        assertThat(evento.cantidad()).isEqualTo(2);
        assertThat(evento.stockResultante()).isEqualTo(75);
        assertThat(evento.usuario()).isEqualTo("Joaquin Moya");
        assertThat(evento.rol()).isEqualTo("CLIENTE");
        assertThat(validator.validate(evento)).isEmpty();
    }

    @Test
    void productoCreadoUsaElStockInicialComoCantidad() {
        publisher.productoCreado(producto(5L, 50), new Actor("Admin", "ADMIN_GENERAL"));

        MovimientoInventarioEvento evento = registroEnviado().value();
        assertThat(evento.tipo()).isEqualTo(TipoMovimiento.PRODUCTO_CREADO);
        assertThat(evento.cantidad()).isEqualTo(50);
        assertThat(evento.stockResultante()).isEqualTo(50);
        assertThat(evento.rol()).isEqualTo("ADMIN_GENERAL");
        assertThat(validator.validate(evento)).isEmpty();
    }

    @Test
    void stockRepuestoLlevaCantidadYStockResultante() {
        publisher.stockRepuesto(producto(5L, 12), 5, new Actor("Admin", "ADMIN_GENERAL"));

        ProducerRecord<String, MovimientoInventarioEvento> registro = registroEnviado();
        assertThat(registro.key()).isEqualTo("5");
        assertThat(registro.value().tipo()).isEqualTo(TipoMovimiento.STOCK_REPUESTO);
        assertThat(registro.value().cantidad()).isEqualTo(5);
        assertThat(registro.value().stockResultante()).isEqualTo(12);
    }

    @Test
    void conTransaccionActivaPublicaSoloDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.stockDescontado(producto(1L, 75), 2, cliente);
        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }

    @Test
    void siKafkaFallaAlEnviarNoRompeLaRequest() {
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenThrow(new KafkaException("broker caido"));

        assertThatCode(() -> publisher.stockDescontado(producto(1L, 75), 2, cliente)).doesNotThrowAnyException();
    }

    @Test
    void siElBrokerRechazaElEventoSoloSeRegistra() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("timeout")));

        assertThatCode(() -> publisher.stockDescontado(producto(1L, 75), 2, cliente)).doesNotThrowAnyException();
    }

    /** Con la configuracion de application.yml el JSON calza con el contrato y no lleva headers de tipo. */
    @Test
    void elSerializerEscribeFechaIsoSinHeadersDeTipo() {
        MovimientoInventarioEvento evento = new MovimientoInventarioEvento("e-1", TipoMovimiento.STOCK_DESCONTADO,
                Instant.parse("2026-11-17T22:30:00Z"), 1L, "local-01", "local-01-PAN", "Pan amasado", 2, 75,
                "Joaquin Moya", "CLIENTE");

        try (JacksonJsonSerializer<MovimientoInventarioEvento> serializer = new JacksonJsonSerializer<>()) {
            serializer.configure(Map.of("spring.json.add.type.headers", false), false);
            RecordHeaders headers = new RecordHeaders();
            String json = new String(serializer.serialize(TOPICO, headers, evento), StandardCharsets.UTF_8);

            assertThat(json)
                    .contains("\"tipo\":\"STOCK_DESCONTADO\"")
                    .contains("\"fecha\":\"2026-11-17T22:30:00Z\"")
                    .contains("\"productoId\":1")
                    .contains("\"stockResultante\":75");
            assertThat(headers.toArray()).isEmpty();
        }
    }

    private ProducerRecord<String, MovimientoInventarioEvento> registroEnviado() {
        ArgumentCaptor<ProducerRecord<String, MovimientoInventarioEvento>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        return captor.getValue();
    }

    /** Producto ya movido: stock es el que quedo tras el movimiento. */
    private Producto producto(Long id, int stock) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setLocalId("local-01");
        producto.setSku("local-01-PAN");
        producto.setNombre("Pan amasado");
        producto.setPrecio(new BigDecimal("1500"));
        producto.setStock(stock);
        return producto;
    }
}
