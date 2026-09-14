package cl.duocuc.pedidos360.productos.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductoNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ProductoNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpoError(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleStockInsuficiente(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpoError(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = cuerpoError("Datos invalidos");
        Map<String, String> campos = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> campos.put(error.getField(), error.getDefaultMessage()));
        body.put("errores", campos);
        return ResponseEntity.badRequest().body(body);
    }

    private Map<String, Object> cuerpoError(String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("mensaje", mensaje);
        return body;
    }
}
