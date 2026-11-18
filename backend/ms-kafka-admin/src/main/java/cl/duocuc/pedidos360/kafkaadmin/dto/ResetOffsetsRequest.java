package cl.duocuc.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Topico cuyos offsets se mueven (todas sus particiones) y la estrategia. */
public record ResetOffsetsRequest(
        @NotBlank(message = "es obligatorio") @Pattern(regexp = Nombres.PATRON_TOPICO, message = Nombres.MENSAJE_TOPICO) String topico,
        @NotNull(message = "es obligatorio (EARLIEST o LATEST)") EstrategiaReset estrategia) {
}
