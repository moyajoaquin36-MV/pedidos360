package cl.duocuc.pedidos360.kafkaadmin.service;

/** Peticion bien formada pero que Kafka no permite, ej. reducir particiones (400). */
public class OperacionInvalidaException extends RuntimeException {

    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
