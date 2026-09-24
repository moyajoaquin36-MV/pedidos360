package cl.duocuc.pedidos360.productos.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador del local (una de las 20 pymes asociadas) dueno del producto. */
    private String localId;

    private String sku;

    private String nombre;

    private BigDecimal precio;

    /** Inventario basico: unidades disponibles del producto o materia prima principal. */
    private Integer stock;

    public void reponerStock(int cantidad) {
        stock += cantidad;
    }

    public void rebajarStock(int cantidad) {
        if (cantidad > stock) {
            throw new IllegalStateException("Stock insuficiente para " + nombre);
        }
        stock -= cantidad;
    }
}
