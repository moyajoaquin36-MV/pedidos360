package cl.duocuc.pedidos360.kafkaadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Topico nuevo. configs es opcional (solo claves de ConfigsTopico.PERMITIDAS);
 * lo que no se indique queda con el valor por defecto del broker.
 */
public record CrearTopicoRequest(
        @NotBlank(message = "es obligatorio") @Pattern(regexp = Nombres.PATRON_TOPICO, message = Nombres.MENSAJE_TOPICO) String nombre,
        @NotNull(message = "es obligatorio (1 a 12)")
        @Min(value = 1, message = "debe estar entre 1 y 12") @Max(value = 12, message = "debe estar entre 1 y 12") Integer particiones,
        @NotNull(message = "es obligatorio (1 a 3)")
        @Min(value = 1, message = "debe estar entre 1 y 3") @Max(value = 3, message = "debe estar entre 1 y 3") Integer replicas,
        @ConfigsValidas Map<String, String> configs) {

    public Map<String, String> configsOVacias() {
        return configs == null ? Map.of() : configs;
    }

    /** Con min.insync.replicas mayor que replicas el topico no aceptaria escrituras con acks=all. */
    @AssertTrue(message = "min.insync.replicas no puede ser mayor que replicas (el topico no aceptaria escrituras con acks=all)")
    public boolean isMinInsyncCompatible() {
        Integer minInsync = ConfigsTopico.minInsyncReplicas(configs);
        return minInsync == null || replicas == null || minInsync <= replicas;
    }
}
