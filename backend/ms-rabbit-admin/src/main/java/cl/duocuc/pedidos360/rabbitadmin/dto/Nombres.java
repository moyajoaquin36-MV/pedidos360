package cl.duocuc.pedidos360.rabbitadmin.dto;

/** Reglas de nombres compartidas por los DTO y los path variables. */
public final class Nombres {

    /** Letras, numeros, '.', '_' o '-'; sin el prefijo "amq." que RabbitMQ reserva. */
    public static final String PATRON = "^(?!amq\\.)[A-Za-z0-9._-]{1,200}$";
    public static final String MENSAJE = "solo letras, numeros, '.', '_' o '-' (1 a 200 caracteres) y sin el prefijo reservado 'amq.'";

    /** Routing key: puede ser vacia (fanout) e incluir comodines topic '*' y '#'. */
    public static final String PATRON_ROUTING_KEY = "^[A-Za-z0-9._*#-]{0,200}$";
    public static final String MENSAJE_ROUTING_KEY = "solo letras, numeros, '.', '_', '-', '*' o '#' (hasta 200 caracteres)";

    /** Dominio de una topologia (ej. "orders"): minusculas, numeros y '-', sin puntos. */
    public static final String PATRON_DOMINIO = "^[a-z0-9-]{1,50}$";
    public static final String MENSAJE_DOMINIO = "solo minusculas, numeros o '-' (1 a 50 caracteres)";

    private Nombres() {
    }
}
