package cl.duocuc.pedidos360.kafkaadmin.dto;

/**
 * A donde mover los offsets de un grupo detenido.
 * EARLIEST: al primer mensaje retenido (reprocesa todo lo que aun guarda el topico).
 * LATEST:   al final (descarta lo pendiente y solo lee eventos nuevos).
 */
public enum EstrategiaReset {
    EARLIEST,
    LATEST
}
