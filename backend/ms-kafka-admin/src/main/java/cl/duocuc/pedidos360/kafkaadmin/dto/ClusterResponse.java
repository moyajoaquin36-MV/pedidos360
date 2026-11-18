package cl.duocuc.pedidos360.kafkaadmin.dto;

import java.util.List;

/** controlador es null si el cluster aun no tiene uno elegido. */
public record ClusterResponse(String clusterId, Broker controlador, List<Broker> brokers) {

    public record Broker(int id, String host, int puerto) {
    }
}
