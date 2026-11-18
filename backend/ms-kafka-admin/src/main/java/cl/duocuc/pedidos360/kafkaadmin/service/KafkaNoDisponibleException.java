package cl.duocuc.pedidos360.kafkaadmin.service;

/** Kafka no respondio a tiempo o no se pudo crear el cliente (503). */
public class KafkaNoDisponibleException extends RuntimeException {

    public KafkaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
