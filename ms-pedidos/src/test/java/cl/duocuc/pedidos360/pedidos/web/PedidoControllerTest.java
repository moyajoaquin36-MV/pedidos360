package cl.duocuc.pedidos360.pedidos.web;

import cl.duocuc.pedidos360.pedidos.client.ProductosClient;
import cl.duocuc.pedidos360.pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simula el token de Azure AD con jwt().jwt(...) en lugar de @WithMockUser,
 * de modo que el @AuthenticationPrincipal Jwt del controller reciba un
 * principal real (claim "sub" y "roles"), igual que en produccion.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PedidoRepository pedidoRepository;

    @MockitoBean
    private ProductosClient productosClient;

    @BeforeEach
    void limpiarDatos() {
        pedidoRepository.deleteAll();
    }

    @Test
    void sinAutenticacionRetorna401() throws Exception {
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteAutenticadoPuedeCrearYListarPedidos() throws Exception {
        String body = """
                {
                  "localId": "local-01",
                  "modalidadEntrega": "RETIRO_EN_TIENDA",
                  "items": [
                    {"productoId": 1, "nombreProducto": "Pan amasado", "cantidad": 2, "precioUnitario": 1500}
                  ]
                }
                """;

        mockMvc.perform(post("/api/pedidos")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("cliente-123").claim("roles", java.util.List.of("CLIENTE")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/pedidos")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("cliente-123").claim("roles", java.util.List.of("CLIENTE")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isOk());
    }

    @Test
    void repartidorNoPuedeCrearPedidos() throws Exception {
        String body = """
                {
                  "localId": "local-01",
                  "modalidadEntrega": "DELIVERY",
                  "items": [{"productoId": 1, "nombreProducto": "Pan", "cantidad": 1, "precioUnitario": 1000}]
                }
                """;

        mockMvc.perform(post("/api/pedidos")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("repartidor-1").claim("roles", java.util.List.of("REPARTIDOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_REPARTIDOR")))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinStockSuficienteRetorna409YNoCreaElPedido() throws Exception {
        doThrow(new StockInsuficienteException("Stock insuficiente para Pan amasado"))
                .when(productosClient).descontarStock(anyString(), any());

        String body = """
                {
                  "localId": "local-01",
                  "modalidadEntrega": "RETIRO_EN_TIENDA",
                  "items": [{"productoId": 1, "nombreProducto": "Pan amasado", "cantidad": 999, "precioUnitario": 1500}]
                }
                """;

        mockMvc.perform(post("/api/pedidos")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("cliente-123").claim("roles", java.util.List.of("CLIENTE")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());

        org.junit.jupiter.api.Assertions.assertEquals(0, pedidoRepository.count());
    }
}
