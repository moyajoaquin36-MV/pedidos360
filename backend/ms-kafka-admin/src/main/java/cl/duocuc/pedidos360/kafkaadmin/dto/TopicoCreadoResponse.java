package cl.duocuc.pedidos360.kafkaadmin.dto;

import java.util.List;

/** Lo que confirmo Kafka al crear el topico (configs = las de ConfigsTopico.PERMITIDAS). */
public record TopicoCreadoResponse(String nombre, int particiones, int replicas, List<ConfigResponse> configs) {
}
