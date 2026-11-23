package cl.duocuc.pedidos360.kafkaadmin.controller;

import cl.duocuc.pedidos360.kafkaadmin.service.GrupoActivoException;
import cl.duocuc.pedidos360.kafkaadmin.service.KafkaNoDisponibleException;
import cl.duocuc.pedidos360.kafkaadmin.service.OperacionInvalidaException;
import cl.duocuc.pedidos360.kafkaadmin.service.RecursoNoEncontradoException;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownMemberIdException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String GRUPO_ACTIVO = "El grupo tiene consumidores activos: detenlos antes de mover sus offsets";
    private static final String NO_DISPONIBLE = "Kafka no esta disponible o no respondio a tiempo";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = cuerpoError("Datos invalidos");
        Map<String, String> campos = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> campos.put(error.getField(), error.getDefaultMessage()));
        body.put("errores", campos);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Path variables invalidos (ej. DELETE /api/kafka/topics/__consumer_offsets). Si el
     * metodo tambien recibe un @Valid @RequestBody, sus errores llegan por aqui.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleParametrosInvalidos(HandlerMethodValidationException ex) {
        Map<String, Object> body = cuerpoError("Datos invalidos");
        Map<String, String> errores = new HashMap<>();
        ex.getParameterValidationResults().forEach(resultado -> resultado.getResolvableErrors().forEach(error ->
                errores.put(error instanceof FieldError campo ? campo.getField() : resultado.getMethodParameter().getParameterName(),
                        error.getDefaultMessage())));
        body.put("errores", errores);
        return ResponseEntity.badRequest().body(body);
    }

    /** JSON mal formado, numeros con texto o estrategia inexistente (ej. "estrategia": "AYER"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(cuerpoError(
                "Cuerpo invalido: revisa el JSON, que particiones/replicas/total sean enteros y que 'estrategia' sea EARLIEST o LATEST"));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpoError(ex.getMessage()));
    }

    @ExceptionHandler(GrupoActivoException.class)
    public ResponseEntity<Map<String, Object>> handleGrupoActivo(GrupoActivoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpoError(ex.getMessage()));
    }

    @ExceptionHandler(OperacionInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleOperacionInvalida(OperacionInvalidaException ex) {
        return ResponseEntity.badRequest().body(cuerpoError(ex.getMessage()));
    }

    @ExceptionHandler(KafkaNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleNoDisponible(KafkaNoDisponibleException ex) {
        log.warn("Kafka no disponible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(cuerpoError(ex.getMessage()));
    }

    /**
     * Errores devueltos por Kafka. El AdminClient responde con futuros: si llega una
     * ExecutionException se desenvuelve para decidir el codigo por la causa real.
     * "detalle" lleva el mensaje original de Kafka.
     */
    @ExceptionHandler({KafkaException.class, ExecutionException.class, CompletionException.class})
    public ResponseEntity<Map<String, Object>> handleKafka(Exception ex) {
        Throwable causa = desenvolver(ex);
        return switch (causa) {
            case UnknownTopicOrPartitionException e -> errorKafka(HttpStatus.NOT_FOUND, "El topico no existe", e);
            case GroupIdNotFoundException e -> errorKafka(HttpStatus.NOT_FOUND, "El grupo de consumidores no existe", e);
            case TopicExistsException e -> errorKafka(HttpStatus.CONFLICT, "El topico ya existe", e);
            case GroupNotEmptyException e -> errorKafka(HttpStatus.CONFLICT, GRUPO_ACTIVO, e);
            // Kafka responde UNKNOWN_MEMBER_ID al mover offsets de un grupo con consumidores conectados
            case UnknownMemberIdException e -> errorKafka(HttpStatus.CONFLICT, GRUPO_ACTIVO, e);
            case InvalidReplicationFactorException e -> errorKafka(HttpStatus.BAD_REQUEST,
                    "Factor de replicacion invalido: no puede superar la cantidad de brokers disponibles", e);
            case InvalidPartitionsException e -> errorKafka(HttpStatus.BAD_REQUEST, "Numero de particiones invalido", e);
            // Incluye InvalidTopicException (nombre que Kafka no acepta, ej. "." o "..")
            case InvalidConfigurationException e -> errorKafka(HttpStatus.BAD_REQUEST, "Kafka rechazo la configuracion", e);
            case InvalidRequestException e -> errorKafka(HttpStatus.BAD_REQUEST, "Kafka rechazo la peticion", e);
            // TimeoutException, broker caido, sin lider, etc.
            case RetriableException e -> errorKafka(HttpStatus.SERVICE_UNAVAILABLE, NO_DISPONIBLE, e);
            case ApiException e -> errorKafka(HttpStatus.CONFLICT, "Kafka rechazo la operacion", e);
            default -> errorKafka(HttpStatus.SERVICE_UNAVAILABLE, NO_DISPONIBLE, causa);
        };
    }

    private static Throwable desenvolver(Throwable ex) {
        Throwable actual = ex;
        while ((actual instanceof ExecutionException || actual instanceof CompletionException) && actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual;
    }

    private ResponseEntity<Map<String, Object>> errorKafka(HttpStatus status, String mensaje, Throwable causa) {
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            log.warn("Kafka no disponible: {}", causa.toString());
        }
        Map<String, Object> body = cuerpoError(mensaje);
        body.put("detalle", causa.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> cuerpoError(String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("mensaje", mensaje);
        return body;
    }
}
