package cl.duocuc.pedidos360.rabbitadmin.service;

public class RecursoExistenteException extends RuntimeException {

    public RecursoExistenteException(String mensaje) {
        super(mensaje);
    }
}
