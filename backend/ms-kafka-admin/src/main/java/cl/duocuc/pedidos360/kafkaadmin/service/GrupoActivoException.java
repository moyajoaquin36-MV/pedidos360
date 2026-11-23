package cl.duocuc.pedidos360.kafkaadmin.service;

/** Se intento mover offsets de un grupo con consumidores conectados (409). */
public class GrupoActivoException extends RuntimeException {

    public GrupoActivoException(String mensaje) {
        super(mensaje);
    }
}
