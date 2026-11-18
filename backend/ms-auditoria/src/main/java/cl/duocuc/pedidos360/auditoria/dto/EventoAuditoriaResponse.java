package cl.duocuc.pedidos360.auditoria.dto;

import cl.duocuc.pedidos360.auditoria.domain.EventoAuditoria;
import cl.duocuc.pedidos360.auditoria.domain.TipoEntidad;
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.Instant;

/** Registro de la bitacora; payload va como JSON (no como texto escapado). */
public record EventoAuditoriaResponse(
        Long id,
        String eventId,
        String tipo,
        TipoEntidad entidad,
        Long entidadId,
        String localId,
        String usuario,
        String rol,
        Instant fechaEvento,
        Instant recibidoEn,
        String topico,
        int particion,
        long offset,
        String clave,
        @JsonRawValue String payload) {

    public static EventoAuditoriaResponse desde(EventoAuditoria e) {
        return new EventoAuditoriaResponse(e.getId(), e.getEventId(), e.getTipo(), e.getEntidad(), e.getEntidadId(),
                e.getLocalId(), e.getUsuario(), e.getRol(), e.getFechaEvento(), e.getRecibidoEn(), e.getTopico(),
                e.getParticion(), e.getOffset(), e.getClave(), e.getPayload());
    }
}
