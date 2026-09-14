package cl.duocuc.pedidos360.productos.web;

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
    void adminLocalPuedeCrearYClientePuedeListar() throws Exception {
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
                                .jwt(jwt -> jwt.subject("admin-1").claim("roles", java.util.List.of("ADMIN_LOCAL")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN_LOCAL")))
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
}
