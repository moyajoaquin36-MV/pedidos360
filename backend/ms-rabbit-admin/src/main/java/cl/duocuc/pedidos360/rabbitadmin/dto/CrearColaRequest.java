package cl.duocuc.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * durable y conDlq son opcionales y por defecto true: una cola nueva sobrevive
 * reinicios y sus mensajes rechazados van a "nombre.dlq".
 */
public record CrearColaRequest(
        @NotBlank(message = "es obligatorio") @Pattern(regexp = Nombres.PATRON, message = Nombres.MENSAJE) String nombre,
        Boolean durable,
        Boolean conDlq) {

    public boolean esDurable() {
        return durable == null || durable;
    }

    public boolean llevaDlq() {
        return conDlq == null || conDlq;
    }
}
