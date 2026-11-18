package cl.duocuc.pedidos360.kafkaadmin.dto;

import java.util.List;

/** Detalle de un topico: estado de cada particion y sus configuraciones relevantes. */
public record TopicoDetalleResponse(String nombre, boolean interno, int replicas,
                                    List<Particion> particiones, List<ConfigResponse> configs) {

    /** lider es null si la particion no tiene lider (broker caido); isr = replicas sincronizadas. */
    public record Particion(int id, Integer lider, List<Integer> replicas, List<Integer> isr) {
    }
}
