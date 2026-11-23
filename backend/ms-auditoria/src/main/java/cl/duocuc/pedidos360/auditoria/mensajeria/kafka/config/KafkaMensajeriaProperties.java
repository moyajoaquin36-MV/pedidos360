package cl.duocuc.pedidos360.auditoria.mensajeria.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de topicos, configuracion de los DLT y politica de reintentos,
 * todos en application.yml (prefijo "kafka"). Ninguna clase escribe estos
 * valores a mano.
 */
@ConfigurationProperties(prefix = "kafka")
public record KafkaMensajeriaProperties(Topicos topicos, Reintentos reintentos) {

    public record Topicos(String pedidosEventos, String inventarioMovimientos, Dlt dlt) {
    }

    /** Topico de error: mismas particiones que el original para publicar en la misma particion. */
    public record Dlt(String sufijo, int particiones, int replicas, int minInsyncReplicas,
                      String cleanupPolicy, long retentionMs) {
    }

    /** Backoff exponencial: maximo reintentos, intervalo inicial, multiplicador y tope. */
    public record Reintentos(int maximo, long intervaloInicialMs, double multiplicador, long intervaloMaximoMs) {
    }

    /** pedidos-eventos -> pedidos-eventos.DLT */
    public String dlt(String topico) {
        return topico + topicos.dlt().sufijo();
    }
}
