package cl.duocuc.pedidos360.kafkaadmin.controller;

import cl.duocuc.pedidos360.kafkaadmin.dto.TopicoCreadoResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.TopicoResponse;
import cl.duocuc.pedidos360.kafkaadmin.service.GrupoActivoException;
import cl.duocuc.pedidos360.kafkaadmin.service.KafkaAdminService;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KafkaAdminControllerTest {

    private static final String TOPICO_OK = """
            {"nombre":"pedidos-eventos","particiones":3,"replicas":3,
             "configs":{"cleanup.policy":"delete","retention.ms":"604800000","min.insync.replicas":"2"}}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KafkaAdminService service;

    // ---- Seguridad ----

    @Test
    void sinTokenRetorna401() throws Exception {
        mockMvc.perform(post("/api/kafka/topics").contentType(MediaType.APPLICATION_JSON).content(TOPICO_OK))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/kafka/topics")).andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoPuedeAdministrarKafka() throws Exception {
        mockMvc.perform(conRol(post("/api/kafka/topics"), "CLIENTE").content(TOPICO_OK))
                .andExpect(status().isForbidden());
        mockMvc.perform(conRol(get("/api/kafka/consumer-groups"), "CLIENTE"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    // ---- Casos felices ----

    @Test
    void adminCreaTopico() throws Exception {
        when(service.crearTopico(any())).thenReturn(new TopicoCreadoResponse("pedidos-eventos", 3, 3, List.of()));

        mockMvc.perform(conRol(post("/api/kafka/topics"), "ADMIN_GENERAL").content(TOPICO_OK))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("pedidos-eventos"))
                .andExpect(jsonPath("$.replicas").value(3));
    }

    @Test
    void configNumericaSeAceptaComoTexto() throws Exception {
        when(service.crearTopico(any())).thenReturn(new TopicoCreadoResponse("t", 1, 1, List.of()));

        mockMvc.perform(conRol(post("/api/kafka/topics"), "ADMIN_GENERAL")
                        .content("{\"nombre\":\"t\",\"particiones\":1,\"replicas\":1,\"configs\":{\"retention.ms\":60000}}"))
                .andExpect(status().isCreated());
    }

    @Test
    void adminListaTopicosSinInternosPorDefecto() throws Exception {
        when(service.listarTopicos(false)).thenReturn(List.of(new TopicoResponse("pedidos-eventos", 3, 3)));

        mockMvc.perform(conRol(get("/api/kafka/topics"), "ADMIN_GENERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("pedidos-eventos"))
                .andExpect(jsonPath("$[0].particiones").value(3));
    }

    @Test
    void detalleDeTopicoInternoSePuedeConsultar() throws Exception {
        mockMvc.perform(conRol(get("/api/kafka/topics/__consumer_offsets"), "ADMIN_GENERAL"))
                .andExpect(status().isOk());
        verify(service).obtenerTopico("__consumer_offsets");
    }

    @Test
    void eliminarTopicoRetorna204() throws Exception {
        mockMvc.perform(conRol(delete("/api/kafka/topics/pedidos-eventos.DLT"), "ADMIN_GENERAL"))
                .andExpect(status().isNoContent());
        verify(service).eliminarTopico("pedidos-eventos.DLT");
    }

    // ---- Validaciones 400 ----

    @Test
    void nombreVacioOInternoSeRechaza() throws Exception {
        mockMvc.perform(conRol(post("/api/kafka/topics"), "ADMIN_GENERAL").content("{\"nombre\":\"\",\"particiones\":1,\"replicas\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
        mockMvc.perform(conRol(post("/api/kafka/topics"), "ADMIN_GENERAL").content("{\"nombre\":\"__consumer_offsets\",\"particiones\":1,\"replicas\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
        mockMvc.perform(conRol(delete("/api/kafka/topics/__consumer_offsets"), "ADMIN_GENERAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
        verify(service, never()).crearTopico(any());
        verify(service, never()).eliminarTopico(anyString());
    }

    @Test
    void particionesYReplicasFueraDeRangoSeRechazan() throws Exception {
        mockMvc.perform(conRol(post("/api/kafka/topics"), "ADMIN_GENERAL").content("{\"nombre\":\"t\",\"particiones\":0,\"replicas\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.particiones").exists());
        mockMvc.perform(conRol(post("/api/kafka/topics"), "ADMIN_GENERAL").content("{\"nombre\":\"t\",\"particiones\":3,\"replicas\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.replicas").exists());
        verify(service, never()).crearTopico(any());
    }

    @Test
    void configsNoPermitidasOInvalidasSeRechazan() throws Exception {
        mockMvc.perform(conRol(post("/api/kafka/topics"), "ADMIN_GENERAL")
                        .content("{\"nombre\":\"t\",\"particiones\":1,\"replicas\":1,\"configs\":{\"unclean.leader.election.enable\":\"true\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.configs").value(containsString("no esta permitida")));
        mockMvc.perform(conRol(post("/api/kafka/topics"), "ADMIN_GENERAL")
                        .content("{\"nombre\":\"t\",\"particiones\":1,\"replicas\":1,\"configs\":{\"retention.ms\":\"1000\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.configs").value(containsString("retention.ms")));
        mockMvc.perform(conRol(post("/api/kafka/topics"), "ADMIN_GENERAL")
                        .content("{\"nombre\":\"t\",\"particiones\":3,\"replicas\":1,\"configs\":{\"min.insync.replicas\":\"2\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.minInsyncCompatible").exists());
        mockMvc.perform(conRol(patch("/api/kafka/topics/pedidos-eventos/configs"), "ADMIN_GENERAL")
                        .content("{\"configs\":{\"retention.ms\":\"10\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.configs").exists());
        verify(service, never()).crearTopico(any());
        verify(service, never()).actualizarConfigs(anyString(), anyMap());
    }

    @Test
    void particionesNuevasFueraDeRangoSeRechazan() throws Exception {
        mockMvc.perform(conRol(post("/api/kafka/topics/pedidos-eventos/particiones"), "ADMIN_GENERAL").content("{\"total\":13}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.total").exists());
        verify(service, never()).aumentarParticiones(anyString(), anyInt());
    }

    @Test
    void estrategiaInvalidaSeRechaza() throws Exception {
        mockMvc.perform(conRol(post("/api/kafka/consumer-groups/grupo-reporteria/offsets"), "ADMIN_GENERAL")
                        .content("{\"topico\":\"pedidos-eventos\",\"estrategia\":\"AYER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("EARLIEST o LATEST")));
        mockMvc.perform(conRol(post("/api/kafka/consumer-groups/grupo-reporteria/offsets"), "ADMIN_GENERAL")
                        .content("{\"topico\":\"pedidos-eventos\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.estrategia").exists());
        verify(service, never()).resetearOffsets(anyString(), any());
    }

    // ---- Errores de Kafka ----

    @Test
    void erroresDeKafkaSeTraducenAlCodigoHttp() throws Exception {
        when(service.crearTopico(any())).thenThrow(new TopicExistsException("Topic 'pedidos-eventos' already exists."));
        mockMvc.perform(conRol(post("/api/kafka/topics"), "ADMIN_GENERAL").content(TOPICO_OK))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalle").exists());

        when(service.obtenerTopico("no-existe")).thenThrow(new UnknownTopicOrPartitionException("unknown"));
        mockMvc.perform(conRol(get("/api/kafka/topics/no-existe"), "ADMIN_GENERAL"))
                .andExpect(status().isNotFound());

        doThrow(new TimeoutException("Timed out waiting for a node assignment")).when(service).eliminarTopico("lento");
        mockMvc.perform(conRol(delete("/api/kafka/topics/lento"), "ADMIN_GENERAL"))
                .andExpect(status().isServiceUnavailable());

        when(service.resetearOffsets(eq("grupo-auditoria"), any())).thenThrow(new GrupoActivoException("activo"));
        mockMvc.perform(conRol(post("/api/kafka/consumer-groups/grupo-auditoria/offsets"), "ADMIN_GENERAL")
                        .content("{\"topico\":\"pedidos-eventos\",\"estrategia\":\"EARLIEST\"}"))
                .andExpect(status().isConflict());
    }

    private MockHttpServletRequestBuilder conRol(MockHttpServletRequestBuilder request, String rol) {
        return request.contentType(MediaType.APPLICATION_JSON)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol)));
    }
}
