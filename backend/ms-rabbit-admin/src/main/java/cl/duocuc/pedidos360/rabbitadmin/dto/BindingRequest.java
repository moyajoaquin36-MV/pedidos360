package cl.duocuc.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Enlace exchange -> cola. Se usa tanto para crear como para eliminar el binding. */
public record BindingRequest(
        @NotBlank(message = "es obligatorio") @Pattern(regexp = Nombres.PATRON, message = Nombres.MENSAJE) String exchange,
        @NotBlank(message = "es obligatorio") @Pattern(regexp = Nombres.PATRON, message = Nombres.MENSAJE) String cola,
        @NotNull(message = "es obligatorio (puede ser \"\" para fanout)")
        @Pattern(regexp = Nombres.PATRON_ROUTING_KEY, message = Nombres.MENSAJE_ROUTING_KEY) String routingKey) {
}
