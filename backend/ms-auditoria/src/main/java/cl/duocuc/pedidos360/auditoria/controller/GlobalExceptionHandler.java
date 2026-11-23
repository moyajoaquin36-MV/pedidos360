package cl.duocuc.pedidos360.auditoria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** limite fuera de 1..200. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleParametroInvalido(HandlerMethodValidationException ex) {
        Map<String, Object> body = cuerpoError("Parametros invalidos");
        body.put("errores", ex.getAllErrors().stream().map(error -> error.getDefaultMessage()).toList());
        return ResponseEntity.badRequest().body(body);
    }

    /** Ej. limite=abc o pedidoId=x. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(cuerpoError("El parametro '" + ex.getName() + "' tiene un valor invalido"));
    }

    /** pedidoId y productoId a la vez. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleFiltroInvalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(cuerpoError(ex.getMessage()));
    }

    private Map<String, Object> cuerpoError(String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("mensaje", mensaje);
        return body;
    }
}
