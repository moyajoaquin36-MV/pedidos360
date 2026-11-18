package cl.duocuc.pedidos360.kafkaadmin.dto;

/** Grupo de consumidores y su estado (STABLE, EMPTY, PREPARING_REBALANCE, ...). */
public record GrupoResponse(String groupId, String estado) {
}
