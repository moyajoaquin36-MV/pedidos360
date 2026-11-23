package cl.duocuc.pedidos360.auditoria.auditoria;

import cl.duocuc.pedidos360.auditoria.domain.EventoAuditoria;
import cl.duocuc.pedidos360.auditoria.domain.TipoEntidad;
import cl.duocuc.pedidos360.auditoria.mensajeria.kafka.evento.MovimientoInventarioEvento;
import cl.duocuc.pedidos360.auditoria.mensajeria.kafka.evento.PedidoEvento;
import cl.duocuc.pedidos360.auditoria.repository.EventoAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Bitacora de auditoria (logica de negocio, sin Kafka): cada accion sobre un
 * pedido o un producto queda guardada con su fecha, usuario y rol.
 *
 * Idempotente por eventId: un reintento o una relectura no duplica registros.
 * Sin @Transactional a proposito: cada guardado es su propia transaccion, asi
 * un duplicado detectado por la restriccion UNIQUE no deja nada a medias.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private static final Set<String> TIPOS_PEDIDO = Set.of("PEDIDO_CREADO", "ESTADO_CAMBIADO");
    private static final Set<String> TIPOS_INVENTARIO = Set.of("PRODUCTO_CREADO", "STOCK_DESCONTADO", "STOCK_REPUESTO");

    private final EventoAuditoriaRepository repository;
    private final JsonMapper jsonMapper;

    public AuditoriaService(EventoAuditoriaRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    /** Devuelve false si el evento ya estaba auditado (duplicado descartado). */
    public boolean registrarPedido(PedidoEvento evento, OrigenEvento origen) {
        validarTipo(evento.tipo(), TIPOS_PEDIDO, origen);
        return guardar(new EventoAuditoria(evento.eventId(), origen.topico(), origen.particion(), origen.offset(),
                origen.clave(), evento.tipo(), TipoEntidad.PEDIDO, evento.pedidoId(), evento.localId(),
                evento.usuario(), evento.rol(), evento.fecha(), Instant.now(), jsonMapper.writeValueAsString(evento)));
    }

    /** Devuelve false si el evento ya estaba auditado (duplicado descartado). */
    public boolean registrarMovimiento(MovimientoInventarioEvento evento, OrigenEvento origen) {
        validarTipo(evento.tipo(), TIPOS_INVENTARIO, origen);
        return guardar(new EventoAuditoria(evento.eventId(), origen.topico(), origen.particion(), origen.offset(),
                origen.clave(), evento.tipo(), TipoEntidad.PRODUCTO, evento.productoId(), evento.localId(),
                evento.usuario(), evento.rol(), evento.fecha(), Instant.now(), jsonMapper.writeValueAsString(evento)));
    }

    /**
     * Historial mas reciente primero. Filtra por pedido o por producto (no ambos)
     * y opcionalmente por tipo de evento.
     */
    public List<EventoAuditoria> buscar(Long pedidoId, Long productoId, String tipo, int limite) {
        if (pedidoId != null && productoId != null) {
            throw new IllegalArgumentException("Filtra por pedidoId o por productoId, no por ambos");
        }
        Limit max = Limit.of(limite);
        String tipoFiltro = tipo == null || tipo.isBlank() ? null : tipo.trim();
        TipoEntidad entidad = pedidoId != null ? TipoEntidad.PEDIDO : productoId != null ? TipoEntidad.PRODUCTO : null;
        Long entidadId = pedidoId != null ? pedidoId : productoId;

        if (entidad != null && tipoFiltro != null) {
            return repository.findByEntidadAndEntidadIdAndTipoOrderByFechaEventoDescIdDesc(entidad, entidadId, tipoFiltro, max);
        }
        if (entidad != null) {
            return repository.findByEntidadAndEntidadIdOrderByFechaEventoDescIdDesc(entidad, entidadId, max);
        }
        if (tipoFiltro != null) {
            return repository.findByTipoOrderByFechaEventoDescIdDesc(tipoFiltro, max);
        }
        return repository.findAllByOrderByFechaEventoDescIdDesc(max);
    }

    private boolean guardar(EventoAuditoria evento) {
        if (repository.existsByEventId(evento.getEventId())) {
            descartarDuplicado(evento);
            return false;
        }
        try {
            repository.saveAndFlush(evento);
        } catch (DataIntegrityViolationException e) {
            // Otro hilo lo guardo entre el exists y el insert; si no es eso, es un error real.
            if (!repository.existsByEventId(evento.getEventId())) {
                throw e;
            }
            descartarDuplicado(evento);
            return false;
        }
        log.info("Evento auditado eventId={} tipo={} {} #{} usuario={} rol={} fecha={}", evento.getEventId(),
                evento.getTipo(), evento.getEntidad(), evento.getEntidadId(), evento.getUsuario(), evento.getRol(),
                evento.getFechaEvento());
        return true;
    }

    private void descartarDuplicado(EventoAuditoria evento) {
        log.warn("Evento duplicado descartado eventId={} topico={} particion={} offset={}",
                evento.getEventId(), evento.getTopico(), evento.getParticion(), evento.getOffset());
    }

    private void validarTipo(String tipo, Set<String> permitidos, OrigenEvento origen) {
        if (!permitidos.contains(tipo)) {
            throw new EventoInvalidoException("Tipo '" + tipo + "' no corresponde al topico " + origen.topico());
        }
    }
}
