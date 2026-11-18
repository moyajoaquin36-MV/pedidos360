package cl.duocuc.pedidos360.kafkaadmin.dto;

import java.util.List;

/** Offsets que quedaron fijados para el grupo en cada particion del topico. */
public record ResetOffsetsResponse(String groupId, String topico, EstrategiaReset estrategia, List<ParticionOffset> offsets) {

    public record ParticionOffset(int particion, long offset) {
    }
}
