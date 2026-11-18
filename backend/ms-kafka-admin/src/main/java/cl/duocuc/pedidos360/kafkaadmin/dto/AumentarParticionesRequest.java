package cl.duocuc.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Nuevo total de particiones. Kafka solo permite aumentarlas: el servicio
 * rechaza un total menor o igual al actual.
 */
public record AumentarParticionesRequest(
        @NotNull(message = "es obligatorio (2 a 12)")
        @Min(value = 2, message = "debe estar entre 2 y 12") @Max(value = 12, message = "debe estar entre 2 y 12") Integer total) {
}
