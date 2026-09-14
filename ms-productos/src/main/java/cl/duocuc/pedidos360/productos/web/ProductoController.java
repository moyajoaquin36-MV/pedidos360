package cl.duocuc.pedidos360.productos.web;

import cl.duocuc.pedidos360.productos.domain.Producto;
import cl.duocuc.pedidos360.productos.dto.CrearProductoRequest;
import cl.duocuc.pedidos360.productos.dto.ProductoResponse;
import cl.duocuc.pedidos360.productos.dto.RebajarStockRequest;
import cl.duocuc.pedidos360.productos.repository.ProductoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoRepository productoRepository;

    public ProductoController(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @GetMapping
    public List<ProductoResponse> listar() {
        return productoRepository.findAll().stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return ProductoResponse.from(buscarOFallar(id));
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody CrearProductoRequest request) {
        Producto producto = new Producto();
        producto.setLocalId(request.localId());
        producto.setSku(request.sku());
        producto.setNombre(request.nombre());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());

        Producto guardado = productoRepository.save(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductoResponse.from(guardado));
    }

    @PatchMapping("/{id}/stock")
    public ProductoResponse rebajarStock(@PathVariable Long id, @Valid @RequestBody RebajarStockRequest request) {
        Producto producto = buscarOFallar(id);
        producto.rebajarStock(request.cantidad());
        return ProductoResponse.from(productoRepository.save(producto));
    }

    private Producto buscarOFallar(Long id) {
        return productoRepository.findById(id).orElseThrow(() -> new ProductoNotFoundException(id));
    }
}
