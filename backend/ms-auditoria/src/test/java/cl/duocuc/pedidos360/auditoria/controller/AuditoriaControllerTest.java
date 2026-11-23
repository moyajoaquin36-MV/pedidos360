package cl.duocuc.pedidos360.auditoria.controller;

import cl.duocuc.pedidos360.auditoria.auditoria.AuditoriaService;
import cl.duocuc.pedidos360.auditoria.domain.EventoAuditoria;
import cl.duocuc.pedidos360.auditoria.domain.TipoEntidad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditoriaService service;

    @Test
    void sinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/auditoria/eventos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operadorDeCocinaNoVeLaAuditoria() throws Exception {
        mockMvc.perform(conRol(get("/api/auditoria/eventos"), "OPERADOR_COCINA"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminVeElHistorialDeUnPedido() throws Exception {
        EventoAuditoria evento = new EventoAuditoria("e1", "pedidos-eventos", 1, 42L, "9", "PEDIDO_CREADO",
                TipoEntidad.PEDIDO, 9L, "local-01", "Joaquin Moya", "CLIENTE",
                Instant.parse("2026-11-17T22:30:00Z"), Instant.parse("2026-11-17T22:30:01Z"), "{\"pedidoId\":9}");
        when(service.buscar(9L, null, null, 50)).thenReturn(List.of(evento));

        mockMvc.perform(conRol(get("/api/auditoria/eventos?pedidoId=9"), "ADMIN_GENERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("e1"))
                .andExpect(jsonPath("$[0].rol").value("CLIENTE"))
                .andExpect(jsonPath("$[0].payload.pedidoId").value(9));
    }

    @Test
    void limiteFueraDeRangoONoNumericoRetorna400() throws Exception {
        for (String limite : List.of("0", "201", "abc")) {
            mockMvc.perform(conRol(get("/api/auditoria/eventos?limite=" + limite), "ADMIN_GENERAL"))
                    .andExpect(status().isBadRequest());
        }
        verify(service, never()).buscar(any(), any(), any(), anyInt());
    }

    private MockHttpServletRequestBuilder conRol(MockHttpServletRequestBuilder request, String rol) {
        return request.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol)));
    }
}
