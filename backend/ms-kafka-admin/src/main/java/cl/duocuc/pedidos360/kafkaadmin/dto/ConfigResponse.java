package cl.duocuc.pedidos360.kafkaadmin.dto;

/**
 * Valor efectivo de una configuracion del topico.
 * porDefecto: true si el topico no la sobrescribe (hereda del broker o de Kafka).
 * origen:     de donde sale el valor (DYNAMIC_TOPIC_CONFIG, STATIC_BROKER_CONFIG, DEFAULT_CONFIG, ...).
 */
public record ConfigResponse(String nombre, String valor, boolean porDefecto, String origen) {
}
