package cl.duocuc.pedidos360.productos.controller;

import cl.duocuc.pedidos360.productos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoRepository productoRepository;

    @BeforeEach
    void limpiarDatos() {
        productoRepository.deleteAll();
    }

    @Test
    void sinAutenticacionRetorna401() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminPuedeCrearYClientePuedeListar() throws Exception {
        String body = """
                {
                  "localId": "local-01",
                  "sku": "PAN-001",
                  "nombre": "Pan amasado",
                  "precio": 1500,
                  "stock": 50
                }
                """;

        mockMvc.perform(post("/api/productos")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("admin-1").claim("roles", java.util.List.of("ADMIN_GENERAL")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN_GENERAL")))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/productos")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("cliente-1").claim("roles", java.util.List.of("CLIENTE")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeCrearProductos() throws Exception {
        String body = """
                {
                  "localId": "local-01",
                  "sku": "PAN-002",
                  "nombre": "Baguette",
                  "precio": 1200,
                  "stock": 30
                }
                """;

        mockMvc.perform(post("/api/productos")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("cliente-1").claim("roles", java.util.List.of("CLIENTE")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void descuentoDeStockRebajaYRechazaSiNoAlcanza() throws Exception {
        cl.duocuc.pedidos360.productos.domain.Producto p = new cl.duocuc.pedidos360.productos.domain.Producto();
        p.setLocalId("local-01");
        p.setSku("T-1");
        p.setNombre("Pan de prueba");
        p.setPrecio(new java.math.BigDecimal("1000"));
        p.setStock(5);
        p = productoRepository.save(p);

        var jwtCliente = SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject("cliente-1").claim("roles", java.util.List.of("CLIENTE")))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));

        mockMvc.perform(post("/api/productos/descuento-stock").with(jwtCliente)
                        .contentType("application/json")
                        .content("{\"items\":[{\"productoId\":" + p.getId() + ",\"cantidad\":3}]}"))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertEquals(2, productoRepository.findById(p.getId()).orElseThrow().getStock());

        mockMvc.perform(post("/api/productos/descuento-stock").with(jwtCliente)
                        .contentType("application/json")
                        .content("{\"items\":[{\"productoId\":" + p.getId() + ",\"cantidad\":3}]}"))
                .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertEquals(2, productoRepository.findById(p.getId()).orElseThrow().getStock());
    }
}
