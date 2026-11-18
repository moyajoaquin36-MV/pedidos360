package cl.duocuc.pedidos360.auditoria.repository;

import cl.duocuc.pedidos360.auditoria.domain.EventoAuditoria;
import cl.duocuc.pedidos360.auditoria.domain.TipoEntidad;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Consultas de la bitacora: siempre la mas reciente primero y con limite. */
public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {

    boolean existsByEventId(String eventId);

    List<EventoAuditoria> findByEntidadAndEntidadIdOrderByFechaEventoDescIdDesc(TipoEntidad entidad, Long entidadId, Limit limite);

    List<EventoAuditoria> findByEntidadAndEntidadIdAndTipoOrderByFechaEventoDescIdDesc(TipoEntidad entidad, Long entidadId,
                                                                                         String tipo, Limit limite);

    List<EventoAuditoria> findByTipoOrderByFechaEventoDescIdDesc(String tipo, Limit limite);

    List<EventoAuditoria> findAllByOrderByFechaEventoDescIdDesc(Limit limite);
}
