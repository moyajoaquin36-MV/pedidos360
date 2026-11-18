package cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.SerializationUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Los JSON de ejemplo de docs/kafka-contrato.md se leen sin headers de tipo y pasan la validacion. */
class EventosContratoTest {

    private static final String PEDIDO_JSON = """
            {"eventId":"6f0c1d2e-uuid","tipo":"PEDIDO_CREADO","fecha":"2026-11-17T22:30:00Z","pedidoId":9,
             "localId":"local-01","clienteId":"sub-del-jwt","estado":"RECIBIDO","estadoAnterior":null,
             "modalidadEntrega":"RETIRO_EN_TIENDA","total":3000.00,
             "items":[{"productoId":1,"nombreProducto":"Pan amasado","cantidad":2,"precioUnitario":1500.00}],
             "usuario":"Joaquin Moya","rol":"CLIENTE"}""";

    private static final String MOVIMIENTO_JSON = """
            {"eventId":"uuid","tipo":"STOCK_DESCONTADO","fecha":"2026-11-17T22:30:00Z","productoId":1,
             "localId":"local-01","sku":"local-01-PAN","nombre":"Pan amasado","cantidad":2,"stockResultante":75,
             "usuario":"Joaquin Moya","rol":"CLIENTE"}""";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void pedidoDelContratoSeDeserializaYEsValido() {
        PedidoEvento evento = deserializar(PedidoEvento.class, PEDIDO_JSON);

        assertThat(evento.pedidoId()).isEqualTo(9L);
        assertThat(evento.fecha()).isEqualTo(Instant.parse("2026-11-17T22:30:00Z"));
        assertThat(evento.total()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(evento.items()).singleElement().satisfies(i -> assertThat(i.cantidad()).isEqualTo(2));
        assertThat(validator.validate(evento)).isEmpty();
    }

    @Test
    void movimientoDelContratoSeDeserializaYEsValido() {
        MovimientoInventarioEvento evento = deserializar(MovimientoInventarioEvento.class, MOVIMIENTO_JSON);

        assertThat(evento.stockResultante()).isEqualTo(75);
        assertThat(evento.sku()).isEqualTo("local-01-PAN");
        assertThat(validator.validate(evento)).isEmpty();
    }

    @Test
    void camposObligatoriosYTotalNegativoNoPasanLaValidacion() {
        PedidoEvento invalido = new PedidoEvento("", "PEDIDO_CREADO", null, 9L, "local-01", null, "RECIBIDO", null,
                null, new BigDecimal("-1"), null, "u", "CLIENTE");

        assertThat(validator.validate(invalido))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("eventId", "fecha", "total");
    }

    @Test
    void jsonIlegibleNoRompeElConsumoYQuedaMarcadoComoErrorDeDeserializacion() {
        ErrorHandlingDeserializer<PedidoEvento> deserializer =
                new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(PedidoEvento.class, false));
        RecordHeaders headers = new RecordHeaders();

        PedidoEvento resultado = deserializer.deserialize("pedidos-eventos", headers, "esto no es json".getBytes(StandardCharsets.UTF_8));

        assertThat(resultado).isNull();
        assertThat(headers.lastHeader(SerializationUtils.VALUE_DESERIALIZER_EXCEPTION_HEADER)).isNotNull();
    }

    private static <T> T deserializar(Class<T> tipo, String json) {
        try (JacksonJsonDeserializer<T> deserializer = new JacksonJsonDeserializer<>(tipo, false)) {
            return deserializer.deserialize("topico", new RecordHeaders(), json.getBytes(StandardCharsets.UTF_8));
        }
    }
}
