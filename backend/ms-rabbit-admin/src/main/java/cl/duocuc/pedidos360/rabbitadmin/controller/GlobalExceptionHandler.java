package cl.duocuc.pedidos360.rabbitadmin.controller;

import cl.duocuc.pedidos360.rabbitadmin.service.RecursoExistenteException;
import cl.duocuc.pedidos360.rabbitadmin.service.RecursoNoEncontradoException;
import org.springframework.amqp.AmqpException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = cuerpoError("Datos invalidos");
        Map<String, String> campos = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> campos.put(error.getField(), error.getDefaultMessage()));
        body.put("errores", campos);
        return ResponseEntity.badRequest().body(body);
    }

    /** Path variables invalidos, por ejemplo DELETE /api/rabbit/queues/amq.algo */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handlePathInvalido(HandlerMethodValidationException ex) {
        Map<String, Object> body = cuerpoError("Nombre invalido");
        body.put("errores", ex.getAllErrors().stream().map(error -> error.getDefaultMessage()).toList());
        return ResponseEntity.badRequest().body(body);
    }

    /** JSON mal formado o tipo de exchange inexistente (ej. "tipo": "HEADERSX"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(cuerpoError("Cuerpo invalido: revisa el JSON y que 'tipo' sea DIRECT, TOPIC o FANOUT"));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpoError(ex.getMessage()));
    }

    @ExceptionHandler(RecursoExistenteException.class)
    public ResponseEntity<Map<String, Object>> handleExistente(RecursoExistenteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpoError(ex.getMessage()));
    }

    /** RabbitMQ rechazo la operacion (ej. exchange inexistente o ya declarado con otro tipo). */
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, Object>> handleRabbit(AmqpException ex) {
        String causa = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpoError("RabbitMQ rechazo la operacion: " + causa));
    }

    private Map<String, Object> cuerpoError(String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("mensaje", mensaje);
        return body;
    }
}
