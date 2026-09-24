package cl.duocuc.pedidos360.productos.repository;

import cl.duocuc.pedidos360.productos.domain.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByLocalId(String localId);

    /** Bloquea las filas (SELECT ... FOR UPDATE) para que compras simultaneas no vendan stock inexistente. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id in :ids")
    List<Producto> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}
