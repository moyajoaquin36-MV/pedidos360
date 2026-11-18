package cl.duocuc.pedidos360.reportes.controller;

import cl.duocuc.pedidos360.reportes.metricas.MetricasResponse;
import cl.duocuc.pedidos360.reportes.metricas.MetricasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MetricasService metricasService;

    @BeforeEach
    void metricas() {
        when(metricasService.obtenerMetricas()).thenReturn(new MetricasResponse(1, new BigDecimal("3000.00"),
                Map.of("RECIBIDO", 1L), Map.of("local-01", new BigDecimal("3000.00")),
                List.of(new MetricasResponse.ProductoDemandado(1L, "Pan amasado", 2)), List.of(), List.of(), 1,
                Instant.parse("2026-11-17T22:30:00Z")));
    }

    @Test
    void sinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/reportes/metricas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoVeElPanel() throws Exception {
        mockMvc.perform(conRol("CLIENTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminVeLasMetricas() throws Exception {
        mockMvc.perform(conRol("ADMIN_GENERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(1))
                .andExpect(jsonPath("$.topProductos[0].nombreProducto").value("Pan amasado"))
                .andExpect(jsonPath("$.actualizado").exists());
    }

    @Test
    void operadorDeCocinaVeLasMetricas() throws Exception {
        mockMvc.perform(conRol("OPERADOR_COCINA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidosPorEstado.RECIBIDO").value(1));
    }

    private MockHttpServletRequestBuilder conRol(String rol) {
        return get("/api/reportes/metricas").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol)));
    }
}
