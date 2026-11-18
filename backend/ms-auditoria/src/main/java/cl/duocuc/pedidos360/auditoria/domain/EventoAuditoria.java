package cl.duocuc.pedidos360.auditoria.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Registro inalterable de un evento: que paso (tipo, payload), sobre que
 * (entidad), quien (usuario, rol), cuando (fechaEvento) y de donde vino
 * (topico, particion, offset). @Immutable: Hibernate nunca lo actualiza.
 * eventId UNIQUE: el mismo evento no se guarda dos veces.
 */
@Entity
@Immutable
@Table(name = "evento_auditoria",
        uniqueConstraints = @UniqueConstraint(name = "uk_evento_auditoria_event_id", columnNames = "event_id"),
        indexes = {
                @Index(name = "idx_evento_auditoria_entidad", columnList = "entidad, entidad_id, fecha_evento"),
                @Index(name = "idx_evento_auditoria_tipo", columnList = "tipo, fecha_evento")
        })
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(nullable = false, length = 150)
    private String topico;

    private int particion;

    // "offset" es palabra reservada en H2/SQL
    @Column(name = "offset_kafka")
    private long offset;

    private String clave;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEntidad entidad;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    private String localId;

    @Column(nullable = false)
    private String usuario;

    @Column(nullable = false, length = 50)
    private String rol;

    @Column(name = "fecha_evento", nullable = false)
    private Instant fechaEvento;

    @Column(nullable = false)
    private Instant recibidoEn;

    @Lob
    @Column(nullable = false)
    private String payload;

    protected EventoAuditoria() {
    }

    public EventoAuditoria(String eventId, String topico, int particion, long offset, String clave, String tipo,
                           TipoEntidad entidad, Long entidadId, String localId, String usuario, String rol,
                           Instant fechaEvento, Instant recibidoEn, String payload) {
        this.eventId = eventId;
        this.topico = topico;
        this.particion = particion;
        this.offset = offset;
        this.clave = clave;
        this.tipo = tipo;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.localId = localId;
        this.usuario = usuario;
        this.rol = rol;
        this.fechaEvento = fechaEvento;
        this.recibidoEn = recibidoEn;
        this.payload = payload;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTopico() {
        return topico;
    }

    public int getParticion() {
        return particion;
    }

    public long getOffset() {
        return offset;
    }

    public String getClave() {
        return clave;
    }

    public String getTipo() {
        return tipo;
    }

    public TipoEntidad getEntidad() {
        return entidad;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public String getLocalId() {
        return localId;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getRol() {
        return rol;
    }

    public Instant getFechaEvento() {
        return fechaEvento;
    }

    public Instant getRecibidoEn() {
        return recibidoEn;
    }

    public String getPayload() {
        return payload;
    }
}
