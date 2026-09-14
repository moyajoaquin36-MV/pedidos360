package cl.duocuc.pedidos360.productos.repository;

import cl.duocuc.pedidos360.productos.domain.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByLocalId(String localId);
}
