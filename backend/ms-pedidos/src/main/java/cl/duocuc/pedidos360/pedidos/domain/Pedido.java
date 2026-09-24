package cl.duocuc.pedidos360.pedidos.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador del local (una de las 20 pymes asociadas) que recibe el pedido. */
    private String localId;

    /** Correo o subject (claim "sub"/"oid") del cliente autenticado via Azure AD. */
    private String clienteId;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estado = EstadoPedido.RECIBIDO;

    @Enumerated(EnumType.STRING)
    private ModalidadEntrega modalidadEntrega;

    private Instant fechaCreacion = Instant.now();

    private Instant fechaActualizacion = Instant.now();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> items = new ArrayList<>();

    public BigDecimal getTotal() {
        return items.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void agregarItem(ItemPedido item) {
        item.setPedido(this);
        items.add(item);
    }

    public void cambiarEstado(EstadoPedido nuevoEstado) {
        this.estado = nuevoEstado;
        this.fechaActualizacion = Instant.now();
    }
}
