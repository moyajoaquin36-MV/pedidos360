package cl.duocuc.pedidos360.productos.mensajeria.consumidor;

/** Error NO recuperable: reintentar no sirve, el mensaje va directo a la DLQ. */
public class MensajeInvalidoException extends RuntimeException {

    public MensajeInvalidoException(String mensaje) {
        super(mensaje);
    }
}
