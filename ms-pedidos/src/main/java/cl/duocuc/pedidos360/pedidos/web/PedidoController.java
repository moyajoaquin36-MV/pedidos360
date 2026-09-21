package cl.duocuc.pedidos360.pedidos.web;

import cl.duocuc.pedidos360.pedidos.client.ProductosClient;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * open-in-view esta desactivado (application.yml), por lo que la
 * transaccion debe mantenerse abierta explicitamente mientras se mapea la
 * entidad (con su coleccion lazy "items") a PedidoResponse.
 */
@RestController
@RequestMapping("/api/pedidos")
@Transactional
public class PedidoController {

    private static final Set<String> ROLES_PERSONAL_OPERATIVO =
            Set.of("OPERADOR_COCINA", "REPARTIDOR", "ADMIN_LOCAL", "ADMIN_GENERAL");

    private final PedidoRepository pedidoRepository;
    private final ProductosClient productosClient;

    public PedidoController(PedidoRepository pedidoRepository, ProductosClient productosClient) {
        this.pedidoRepository = pedidoRepository;
        this.productosClient = productosClient;
    }

    /**
     * Un CLIENTE solo ve sus propios pedidos (por clienteId = subject del
     * JWT). El personal operativo y los administradores ven todos los
     * pedidos, ya que necesitan coordinar preparacion/despacho entre
     * distintos clientes.
     */
    @GetMapping
    public List<PedidoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        boolean esPersonalOperativo = roles != null && roles.stream().anyMatch(ROLES_PERSONAL_OPERATIVO::contains);

        List<Pedido> pedidos = esPersonalOperativo
                ? pedidoRepository.findAll()
                : pedidoRepository.findByClienteId(jwt.getSubject());

        return pedidos.stream()
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
        // Rebaja de inventario en ms-productos (todo o nada). Si no alcanza el
        // stock responde 409 y el pedido no se crea.
        productosClient.descontarStock(jwt.getTokenValue(), request.items().stream()
                .map(item -> new ProductosClient.ItemStock(item.productoId(), item.cantidad()))
                .toList());

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
