package cl.duocuc.pedidos360.reportes.metricas;

/**
 * Evento con datos incoherentes para el negocio (ej. total negativo).
 * Reintentar no lo arregla: el error handler lo manda directo al DLT.
 */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
