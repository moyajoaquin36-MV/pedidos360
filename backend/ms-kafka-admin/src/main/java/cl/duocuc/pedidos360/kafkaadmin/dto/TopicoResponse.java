package cl.duocuc.pedidos360.kafkaadmin.dto;

/** Resumen de un topico: particiones y factor de replicacion. */
public record TopicoResponse(String nombre, int particiones, int replicas) {
}
