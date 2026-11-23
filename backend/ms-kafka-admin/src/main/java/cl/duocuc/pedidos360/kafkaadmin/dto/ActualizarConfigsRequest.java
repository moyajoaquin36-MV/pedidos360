package cl.duocuc.pedidos360.kafkaadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

/** Configuraciones a fijar en un topico existente; las que no vengan no se tocan. */
public record ActualizarConfigsRequest(
        @NotEmpty(message = "debe traer al menos una configuracion") @ConfigsValidas Map<String, String> configs) {
}
