package cl.duocuc.pedidos360.pedidos.web;

import cl.duocuc.pedidos360.pedidos.domain.ItemPedido;
import cl.duocuc.pedidos360.pedidos.domain.Pedido;
import cl.duocuc.pedidos360.pedidos.dto.ActualizarEstadoRequest;
import cl.duocuc.pedidos360.pedidos.dto.CrearPedidoRequest;
import cl.duocuc.pedidos360.pedidos.dto.PedidoResponse;
import cl.duocuc.pedidos360.pedidos.repository.PedidoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoRepository pedidoRepository;

    public PedidoController(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return pedidoRepository.findAll().stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return PedidoResponse.from(buscarOFallar(id));
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> crear(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody CrearPedidoRequest request) {
        Pedido pedido = new Pedido();
        pedido.setLocalId(request.localId());
        pedido.setModalidadEntrega(request.modalidadEntrega());
        pedido.setClienteId(jwt.getSubject());

        request.items().forEach(itemRequest -> {
            ItemPedido item = new ItemPedido();
            item.setProductoId(itemRequest.productoId());
            item.setNombreProducto(itemRequest.nombreProducto());
            item.setCantidad(itemRequest.cantidad());
            item.setPrecioUnitario(itemRequest.precioUnitario());
            pedido.agregarItem(item);
        });

        Pedido guardado = pedidoRepository.save(pedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.from(guardado));
    }

    @PatchMapping("/{id}/estado")
    public PedidoResponse actualizarEstado(@PathVariable Long id, @Valid @RequestBody ActualizarEstadoRequest request) {
        Pedido pedido = buscarOFallar(id);
        pedido.cambiarEstado(request.estado());
        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    private Pedido buscarOFallar(Long id) {
        return pedidoRepository.findById(id).orElseThrow(() -> new PedidoNotFoundException(id));
    }
}
