package cl.duocuc.pedidos360.productos.controller;

import cl.duocuc.pedidos360.productos.domain.Producto;
import cl.duocuc.pedidos360.productos.dto.CrearProductoRequest;
import cl.duocuc.pedidos360.productos.dto.DescontarStockRequest;
import cl.duocuc.pedidos360.productos.dto.ProductoResponse;
import cl.duocuc.pedidos360.productos.dto.RebajarStockRequest;
import cl.duocuc.pedidos360.productos.dto.ReponerStockRequest;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @PatchMapping("/{id}/reposicion")
    public ProductoResponse reponerStock(@PathVariable Long id, @Valid @RequestBody ReponerStockRequest request) {
        Producto producto = buscarOFallar(id);
        producto.reponerStock(request.cantidad());
        return ProductoResponse.from(productoRepository.save(producto));
    }

    /**
     * Descuenta stock de varios productos en una sola transaccion (todo o nada).
     * Lo invoca ms-pedidos al crear un pedido.
     */
    @PostMapping("/descuento-stock")
    @Transactional
    public ResponseEntity<Void> descontarStock(@Valid @RequestBody DescontarStockRequest request) {
        Map<Long, Integer> cantidadPorProducto = new HashMap<>();
        request.items().forEach(item -> cantidadPorProducto.merge(item.productoId(), item.cantidad(), Integer::sum));

        List<Producto> productos = productoRepository.findAllByIdForUpdate(cantidadPorProducto.keySet());
        if (productos.size() != cantidadPorProducto.size()) {
            Long faltante = cantidadPorProducto.keySet().stream()
                    .filter(id -> productos.stream().noneMatch(p -> p.getId().equals(id)))
                    .findFirst().orElseThrow();
            throw new ProductoNotFoundException(faltante);
        }

        productos.forEach(producto -> producto.rebajarStock(cantidadPorProducto.get(producto.getId())));
        productoRepository.saveAll(productos);
        return ResponseEntity.noContent().build();
    }

    private Producto buscarOFallar(Long id) {
        return productoRepository.findById(id).orElseThrow(() -> new ProductoNotFoundException(id));
    }
}
