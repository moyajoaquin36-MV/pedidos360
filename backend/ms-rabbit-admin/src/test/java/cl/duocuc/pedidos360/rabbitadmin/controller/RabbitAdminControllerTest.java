package cl.duocuc.pedidos360.rabbitadmin.controller;

import cl.duocuc.pedidos360.rabbitadmin.dto.ColaResponse;
import cl.duocuc.pedidos360.rabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RabbitAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RabbitAdminService service;

    @Test
    void sinTokenRetorna401() throws Exception {
        mockMvc.perform(post("/api/rabbitmq/queues").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"prueba\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoPuedeAdministrarColas() throws Exception {
        mockMvc.perform(conRol(post("/api/rabbitmq/queues"), "CLIENTE").content("{\"nombre\":\"prueba\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreaCola() throws Exception {
        when(service.crearCola(any())).thenReturn(new ColaResponse("prueba", 0, 0, "prueba.dlq"));

        mockMvc.perform(conRol(post("/api/rabbitmq/queues"), "ADMIN_GENERAL").content("{\"nombre\":\"prueba\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dlq").value("prueba.dlq"));
    }

    @Test
    void nombreVacioOReservadoSeRechaza() throws Exception {
        mockMvc.perform(conRol(post("/api/rabbitmq/queues"), "ADMIN_GENERAL").content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
        mockMvc.perform(conRol(post("/api/rabbitmq/queues"), "ADMIN_GENERAL").content("{\"nombre\":\"amq.mia\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(conRol(delete("/api/rabbitmq/queues/amq.mia"), "ADMIN_GENERAL"))
                .andExpect(status().isBadRequest());
        verify(service, never()).crearCola(any());
    }

    @Test
    void tipoDeExchangeInvalidoSeRechaza() throws Exception {
        mockMvc.perform(conRol(post("/api/rabbitmq/exchanges"), "ADMIN_GENERAL").content("{\"nombre\":\"x\",\"tipo\":\"RARO\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(conRol(post("/api/rabbitmq/exchanges"), "ADMIN_GENERAL").content("{\"nombre\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipo").exists());
    }

    @Test
    void bindingSinRoutingKeySeRechaza() throws Exception {
        mockMvc.perform(conRol(post("/api/rabbitmq/bindings"), "ADMIN_GENERAL").content("{\"exchange\":\"x\",\"cola\":\"y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.routingKey").exists());
    }

    @Test
    void topologiaExigeDominioValido() throws Exception {
        mockMvc.perform(conRol(post("/api/rabbitmq/topology"), "ADMIN_GENERAL"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(conRol(post("/api/rabbitmq/topology?dominio=Con.Punto"), "ADMIN_GENERAL"))
                .andExpect(status().isBadRequest());
        verify(service, never()).crearTopologia(any());
    }

    private MockHttpServletRequestBuilder conRol(MockHttpServletRequestBuilder request, String rol) {
        return request.contentType(MediaType.APPLICATION_JSON)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol)));
    }
}
