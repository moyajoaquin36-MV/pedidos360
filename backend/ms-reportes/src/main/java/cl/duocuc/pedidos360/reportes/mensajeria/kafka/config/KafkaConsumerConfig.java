package cl.duocuc.pedidos360.reportes.mensajeria.kafka.config;

import cl.duocuc.pedidos360.reportes.mensajeria.kafka.consumidor.Correlacion;
import cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento.MovimientoInventarioEvento;
import cl.duocuc.pedidos360.reportes.mensajeria.kafka.evento.PedidoEvento;
import cl.duocuc.pedidos360.reportes.metricas.EventoInvalidoException;
import jakarta.validation.Validator;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.annotation.KafkaListenerConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consumo de Kafka de ms-reportes: una factory por tipo de evento, manejo de
 * errores con reintentos y DLT, y validacion de payloads con @Valid.
 *
 * Flujo de un mensaje defectuoso (no detiene al consumidor):
 *  - JSON ilegible: ErrorHandlingDeserializer captura el error -> DLT sin reintentos.
 *  - @Valid falla o EventoInvalidoException: dato malo -> DLT sin reintentos.
 *  - Otro error (transitorio): 3 reintentos con backoff 1 s, 2 s, 4 s -> DLT.
 * El DLT es <topico>.DLT en la MISMA particion del registro original.
 */
@Configuration
public class KafkaConsumerConfig implements KafkaListenerConfigurer {

    public static final String FACTORY_PEDIDOS = "pedidosListenerContainerFactory";
    public static final String FACTORY_INVENTARIO = "inventarioListenerContainerFactory";

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    private final KafkaProperties kafkaProperties;
    private final KafkaMensajeriaProperties mensajeria;
    private final Validator validator;

    public KafkaConsumerConfig(KafkaProperties kafkaProperties, KafkaMensajeriaProperties mensajeria, Validator validator) {
        this.kafkaProperties = kafkaProperties;
        this.mensajeria = mensajeria;
        this.validator = validator;
    }

    // ---- Factories tipadas: cada topico se deserializa a su propio record ----

    @Bean(FACTORY_PEDIDOS)
    public ConcurrentKafkaListenerContainerFactory<String, PedidoEvento> pedidosListenerContainerFactory(
            CommonErrorHandler kafkaErrorHandler) {
        return crearFactory(PedidoEvento.class, kafkaErrorHandler);
    }

    @Bean(FACTORY_INVENTARIO)
    public ConcurrentKafkaListenerContainerFactory<String, MovimientoInventarioEvento> inventarioListenerContainerFactory(
            CommonErrorHandler kafkaErrorHandler) {
        return crearFactory(MovimientoInventarioEvento.class, kafkaErrorHandler);
    }

    /**
     * Toma bootstrap, group-id, offsets y propiedades avanzadas de spring.kafka.consumer
     * y fija el deserializador del valor: ErrorHandlingDeserializer envolviendo un
     * JacksonJsonDeserializer del tipo indicado, ignorando headers de tipo (el
     * productor no los envia: cada servicio tiene su propia clase).
     */
    private <T> ConcurrentKafkaListenerContainerFactory<String, T> crearFactory(Class<T> tipo, CommonErrorHandler errorHandler) {
        ErrorHandlingDeserializer<T> valor = new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(tipo, false));
        DefaultKafkaConsumerFactory<String, T> consumerFactory = new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(), new StringDeserializer(), valor);

        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        KafkaProperties.Listener listener = kafkaProperties.getListener();
        if (listener.getConcurrency() != null) {
            factory.setConcurrency(listener.getConcurrency());
        }
        if (listener.getAckMode() != null) {
            factory.getContainerProperties().setAckMode(listener.getAckMode());
        }
        factory.setAutoStartup(listener.isAutoStartup());
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    // ---- Manejo de errores: reintentos + Dead Letter Topic ----

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> dltKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate,
                (registro, ex) -> new TopicPartition(mensajeria.dlt(registro.topic()), registro.partition()));
        return crearErrorHandler(recoverer);
    }

    /** Separado del bean para probar la politica de reintentos sin broker. */
    DefaultErrorHandler crearErrorHandler(ConsumerRecordRecoverer recoverer) {
        KafkaMensajeriaProperties.Reintentos reintentos = mensajeria.reintentos();
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(reintentos.maximo());
        backOff.setInitialInterval(reintentos.intervaloInicialMs());
        backOff.setMultiplier(reintentos.multiplicador());
        backOff.setMaxInterval(reintentos.intervaloMaximoMs());

        ConsumerRecordRecoverer conAlerta = (registro, ex) -> {
            log.error("[ALERTA DLT] topico={} particion={} offset={} clave={} correlationId={} -> {}: {}",
                    registro.topic(), registro.partition(), registro.offset(), registro.key(),
                    Correlacion.desde(registro), mensajeria.dlt(registro.topic()), causa(ex));
            recoverer.accept(registro, ex);
        };

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(conAlerta, backOff);
        // Datos malos: reintentar no los arregla.
        errorHandler.addNotRetryableExceptions(
                DeserializationException.class,
                MethodArgumentNotValidException.class,
                EventoInvalidoException.class);
        errorHandler.setRetryListeners((registro, ex, intento) ->
                log.warn("Fallo al procesar topico={} particion={} offset={} correlationId={} (intento {}): {}",
                        registro.topic(), registro.partition(), registro.offset(),
                        Correlacion.desde(registro), intento, causa(ex)));
        return errorHandler;
    }

    /**
     * Productor del DLT. El valor puede ser el record ya deserializado (fallo de
     * negocio o validacion) o los bytes originales (JSON ilegible, que el
     * DeadLetterPublishingRecoverer toma del DeserializationException): por eso
     * se delega por tipo, byte[] tal cual y cualquier otro objeto como JSON.
     */
    @Bean
    public ProducerFactory<String, Object> dltProducerFactory() {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(),
                new StringSerializer(), serializadorDlt());
    }

    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate(ProducerFactory<String, Object> dltProducerFactory) {
        return new KafkaTemplate<>(dltProducerFactory);
    }

    static DelegatingByTypeSerializer serializadorDlt() {
        Map<Class<?>, Serializer<?>> delegados = new LinkedHashMap<>();
        delegados.put(byte[].class, new ByteArraySerializer());
        delegados.put(Object.class, new JacksonJsonSerializer<>().noTypeInfo());
        return new DelegatingByTypeSerializer(delegados, true);
    }

    // ---- Validacion de @Valid @Payload en los listeners ----

    @Override
    public void configureKafkaListeners(KafkaListenerEndpointRegistrar registrar) {
        registrar.setValidator(new SpringValidatorAdapter(validator));
    }

    private static String causa(Exception ex) {
        return NestedExceptionUtils.getMostSpecificCause(ex).toString();
    }
}
