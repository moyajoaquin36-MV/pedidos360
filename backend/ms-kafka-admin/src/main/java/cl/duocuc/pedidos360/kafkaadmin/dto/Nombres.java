package cl.duocuc.pedidos360.kafkaadmin.dto;

/** Reglas de nombres compartidas por los DTO y los path variables. */
public final class Nombres {

    /**
     * Topico que se puede crear, modificar o borrar: las reglas de Kafka (letras,
     * numeros, '.', '_' o '-', hasta 249) y sin el prefijo "__" de los internos.
     */
    public static final String PATRON_TOPICO = "^(?!__)[a-zA-Z0-9._-]{1,249}$";
    public static final String MENSAJE_TOPICO = "solo letras, numeros, '.', '_' o '-' (1 a 249 caracteres) y sin el prefijo '__' reservado a los topicos internos";

    /** Para consultar el detalle se aceptan tambien los internos (ej. __consumer_offsets). */
    public static final String PATRON_TOPICO_CONSULTA = "^[a-zA-Z0-9._-]{1,249}$";
    public static final String MENSAJE_TOPICO_CONSULTA = "solo letras, numeros, '.', '_' o '-' (1 a 249 caracteres)";

    /** group.id de un grupo de consumidores (ej. grupo-reporteria). */
    public static final String PATRON_GRUPO = "^[a-zA-Z0-9._-]{1,249}$";
    public static final String MENSAJE_GRUPO = "solo letras, numeros, '.', '_' o '-' (1 a 249 caracteres)";

    private Nombres() {
    }
}
