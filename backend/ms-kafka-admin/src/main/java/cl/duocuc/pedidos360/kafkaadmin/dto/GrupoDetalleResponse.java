package cl.duocuc.pedidos360.kafkaadmin.dto;

import java.util.List;

/**
 * Estado de un grupo: quien consume que particion y cuanto le falta (lag).
 * lag = endOffset - offset del grupo; lagTotal suma el de todas las particiones.
 */
public record GrupoDetalleResponse(String groupId, String estado, List<Miembro> miembros,
                                   List<OffsetParticion> offsets, long lagTotal) {

    /** particiones asignadas en formato "topico-particion" (ej. pedidos-eventos-0). */
    public record Miembro(String clientId, String host, List<String> particiones) {
    }

    /** offsetGrupo es null si el grupo aun no confirma offsets en esa particion; lag queda null. */
    public record OffsetParticion(String topico, int particion, Long offsetGrupo, Long endOffset, Long lag) {
    }
}
