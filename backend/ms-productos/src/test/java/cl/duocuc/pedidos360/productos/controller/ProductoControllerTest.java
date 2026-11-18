package cl.duocuc.pedidos360.productos.controller;

import cl.duocuc.pedidos360.productos.mensajeria.kafka.evento.Actor;
import cl.duocuc.pedidos360.productos.mensajeria.kafka.productor.InventarioKafkaPublisher;
import cl.duocuc.pedidos360.productos.mensajeria.productor.InventarioEventosPublisher;
import cl.duocuc.pedidos360.productos.repository.ProductoRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

    @MockitoBean
    private InventarioEventosPublisher eventos;

    @MockitoBean
    private InventarioKafkaPublisher eventosKafka;

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

        verify(eventosKafka).productoCreado(any(), eq(new Actor("admin-1", "ADMIN_GENERAL")));

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

        // Un solo evento: el descuento rechazado no publica. El actor es el cliente (JWT reenviado por ms-pedidos).
        verify(eventosKafka).stockDescontado(any(), eq(3), eq(new Actor("cliente-1", "CLIENTE")));
    }

    @Test
    void rebajaDeStockPorOperadorPublicaElMovimiento() throws Exception {
        cl.duocuc.pedidos360.productos.domain.Producto p = new cl.duocuc.pedidos360.productos.domain.Producto();
        p.setLocalId("local-01");
        p.setSku("S-1");
        p.setNombre("Pan de rebaja");
        p.setPrecio(new java.math.BigDecimal("1000"));
        p.setStock(10);
        p = productoRepository.save(p);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/productos/" + p.getId() + "/stock")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("operador-1").claim("name", "Ana Operadora")
                                        .claim("roles", java.util.List.of("OPERADOR_COCINA")))
                                .authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_COCINA")))
                        .contentType("application/json").content("{\"cantidad\":4}"))
                .andExpect(status().isOk());

        verify(eventosKafka).stockDescontado(any(), eq(4), eq(new Actor("Ana Operadora", "OPERADOR_COCINA")));
    }

    @Test
    void soloAdminPuedeReponerStock() throws Exception {
        cl.duocuc.pedidos360.productos.domain.Producto p = new cl.duocuc.pedidos360.productos.domain.Producto();
        p.setLocalId("local-01");
        p.setSku("R-1");
        p.setNombre("Pan de reposicion");
        p.setPrecio(new java.math.BigDecimal("1000"));
        p.setStock(2);
        p = productoRepository.save(p);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/productos/" + p.getId() + "/reposicion")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("cliente-1").claim("roles", java.util.List.of("CLIENTE")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                        .contentType("application/json").content("{\"cantidad\":5}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/productos/" + p.getId() + "/reposicion")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("admin-1").claim("roles", java.util.List.of("ADMIN_GENERAL")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN_GENERAL")))
                        .contentType("application/json").content("{\"cantidad\":5}"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(7, productoRepository.findById(p.getId()).orElseThrow().getStock());

        verify(eventosKafka).stockRepuesto(any(), eq(5), eq(new Actor("admin-1", "ADMIN_GENERAL")));
    }
}
