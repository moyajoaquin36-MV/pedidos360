package cl.duocuc.pedidos360.auditoria.auditoria;

/**
 * Evento incoherente para la auditoria (ej. tipo desconocido para su topico).
 * Reintentar no lo arregla: el error handler lo manda directo al DLT.
 */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
