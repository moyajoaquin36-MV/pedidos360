package cl.duocuc.pedidos360.kafkaadmin.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lista blanca de configuraciones de topico que la API deja fijar, con la
 * regla de valor de cada una. Son tambien las que muestra el detalle de un topico.
 * Cualquier otra configuracion (ej. unclean.leader.election.enable) se rechaza.
 */
public final class ConfigsTopico {

    public static final List<String> PERMITIDAS = List.of(
            "cleanup.policy", "retention.ms", "retention.bytes", "min.insync.replicas", "segment.ms", "max.message.bytes");

    private static final Set<String> CLEANUP_POLICIES = Set.of("delete", "compact", "compact,delete");
    private static final long UN_MINUTO_MS = 60_000L;

    private ConfigsTopico() {
    }

    /** Errores de las configuraciones recibidas; lista vacia si todas son validas (o no vino ninguna). */
    public static List<String> validar(Map<String, String> configs) {
        List<String> errores = new ArrayList<>();
        if (configs != null) {
            configs.forEach((nombre, valor) -> {
                String error = validar(nombre, valor);
                if (error != null) {
                    errores.add(error);
                }
            });
        }
        return errores;
    }

    /** Error de una configuracion o null si es valida. */
    static String validar(String nombre, String valor) {
        if (!PERMITIDAS.contains(nombre)) {
            return "'" + nombre + "' no esta permitida (permitidas: " + String.join(", ", PERMITIDAS) + ")";
        }
        if (valor == null || valor.isBlank()) {
            return nombre + " debe tener un valor";
        }
        return switch (nombre) {
            case "cleanup.policy" -> CLEANUP_POLICIES.contains(valor) ? null
                    : "cleanup.policy debe ser delete, compact o compact,delete";
            case "retention.ms" -> esMenosUnoODesde(valor, UN_MINUTO_MS) ? null
                    : "retention.ms debe ser -1 (sin limite) o >= 60000 (1 minuto)";
            case "retention.bytes" -> esMenosUnoODesde(valor, 1) ? null
                    : "retention.bytes debe ser -1 (sin limite) o mayor que 0";
            case "min.insync.replicas" -> estaEntre(valor, 1, 3) ? null
                    : "min.insync.replicas debe ser un entero entre 1 y 3";
            case "segment.ms" -> estaEntre(valor, UN_MINUTO_MS, Long.MAX_VALUE) ? null
                    : "segment.ms debe ser >= 60000 (1 minuto)";
            case "max.message.bytes" -> estaEntre(valor, 1024, Integer.MAX_VALUE) ? null
                    : "max.message.bytes debe ser un entero >= 1024";
            default -> null;
        };
    }

    /** min.insync.replicas pedido, o null si no viene o no es numero (eso ya lo informa validar). */
    public static Integer minInsyncReplicas(Map<String, String> configs) {
        Long valor = configs == null ? null : comoNumero(configs.get("min.insync.replicas"));
        return valor == null ? null : valor.intValue();
    }

    private static boolean esMenosUnoODesde(String valor, long minimo) {
        Long numero = comoNumero(valor);
        return numero != null && (numero == -1 || numero >= minimo);
    }

    private static boolean estaEntre(String valor, long minimo, long maximo) {
        Long numero = comoNumero(valor);
        return numero != null && numero >= minimo && numero <= maximo;
    }

    private static Long comoNumero(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
