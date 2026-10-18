package cl.duocuc.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** durable es opcional y por defecto true. */
public record CrearExchangeRequest(
        @NotBlank(message = "es obligatorio") @Pattern(regexp = Nombres.PATRON, message = Nombres.MENSAJE) String nombre,
        @NotNull(message = "es obligatorio (DIRECT, TOPIC o FANOUT)") TipoExchange tipo,
        Boolean durable) {

    public boolean esDurable() {
        return durable == null || durable;
    }
}
