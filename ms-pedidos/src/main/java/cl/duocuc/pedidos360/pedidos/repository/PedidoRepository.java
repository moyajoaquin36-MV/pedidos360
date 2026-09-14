package cl.duocuc.pedidos360.pedidos.repository;

import cl.duocuc.pedidos360.pedidos.domain.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByLocalId(String localId);

    List<Pedido> findByClienteId(String clienteId);
}
