package cl.duocuc.pedidos360.productos.config;

import cl.duocuc.pedidos360.productos.domain.Producto;
import cl.duocuc.pedidos360.productos.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Carga productos de ejemplo para los 20 locales asociados a la red al
 * arrancar, solo si la tabla esta vacia. Pensado para desarrollo/demo local;
 * se puede desactivar con app.seed.enabled=false antes de un despliegue real.
 */
@Configuration
public class DataSeeder {

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    private record ProductoBase(String sku, String nombre, String precio, int stock) {
    }

    /** Mismo tipo de local que en el catalogo estatico del frontend (src/app/data/locales.ts). */
    private static final Map<String, String> TIPO_POR_LOCAL = Map.ofEntries(
            Map.entry("local-01", "PANADERIA"), Map.entry("local-02", "CAFETERIA"), Map.entry("local-03", "PASTELERIA"),
            Map.entry("local-04", "PANADERIA"), Map.entry("local-05", "CAFETERIA"), Map.entry("local-06", "PASTELERIA"),
            Map.entry("local-07", "PANADERIA"), Map.entry("local-08", "CAFETERIA"), Map.entry("local-09", "PASTELERIA"),
            Map.entry("local-10", "PANADERIA"), Map.entry("local-11", "CAFETERIA"), Map.entry("local-12", "PASTELERIA"),
            Map.entry("local-13", "PANADERIA"), Map.entry("local-14", "CAFETERIA"), Map.entry("local-15", "PASTELERIA"),
            Map.entry("local-16", "PANADERIA"), Map.entry("local-17", "CAFETERIA"), Map.entry("local-18", "PASTELERIA"),
            Map.entry("local-19", "PANADERIA"), Map.entry("local-20", "CAFETERIA"));

    private static final Map<String, List<ProductoBase>> PRODUCTOS_POR_TIPO = Map.of(
            "PANADERIA", List.of(
                    new ProductoBase("PAN-01", "Pan amasado", "1500", 80),
                    new ProductoBase("PAN-02", "Marraqueta (unidad)", "150", 300),
                    new ProductoBase("PAN-03", "Hallulla (unidad)", "180", 250)),
            "CAFETERIA", List.of(
                    new ProductoBase("CAF-01", "Cafe americano", "1800", 100),
                    new ProductoBase("CAF-02", "Capuchino", "2200", 100),
                    new ProductoBase("CAF-03", "Sandwich de jamon y queso", "3200", 40)),
            "PASTELERIA", List.of(
                    new ProductoBase("PAS-01", "Torta de chocolate (porcion)", "2500", 20),
                    new ProductoBase("PAS-02", "Kuchen de manzana (porcion)", "2000", 25),
                    new ProductoBase("PAS-03", "Alfajor", "900", 60)));

    @Bean
    public CommandLineRunner sembrarProductos(ProductoRepository productoRepository) {
        return args -> {
            if (!seedEnabled || productoRepository.count() > 0) {
                return;
            }

            TIPO_POR_LOCAL.forEach((localId, tipo) -> {
                for (ProductoBase base : PRODUCTOS_POR_TIPO.get(tipo)) {
                    Producto producto = new Producto();
                    producto.setLocalId(localId);
                    producto.setSku(localId + "-" + base.sku());
                    producto.setNombre(base.nombre());
                    producto.setPrecio(new BigDecimal(base.precio()));
                    producto.setStock(base.stock());
                    productoRepository.save(producto);
                }
            });
        };
    }
}
