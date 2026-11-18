package cl.duocuc.pedidos360.kafkaadmin.controller;

import cl.duocuc.pedidos360.kafkaadmin.dto.ActualizarConfigsRequest;
import cl.duocuc.pedidos360.kafkaadmin.dto.AumentarParticionesRequest;
import cl.duocuc.pedidos360.kafkaadmin.dto.ClusterResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.CrearTopicoRequest;
import cl.duocuc.pedidos360.kafkaadmin.dto.GrupoDetalleResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.GrupoResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.Nombres;
import cl.duocuc.pedidos360.kafkaadmin.dto.ResetOffsetsRequest;
import cl.duocuc.pedidos360.kafkaadmin.dto.ResetOffsetsResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.TopicoCreadoResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.TopicoDetalleResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.TopicoResponse;
import cl.duocuc.pedidos360.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST del administrador de Kafka (solo ADMIN_GENERAL).
 *
 *  GET    /api/kafka/cluster                            clusterId, controlador y brokers
 *  GET    /api/kafka/topics?internos=false              topicos con particiones y factor de replicacion
 *  GET    /api/kafka/topics/{nombre}                    lider/replicas/ISR por particion y configuraciones
 *  POST   /api/kafka/topics                             crea un topico (particiones, replicas, configs)
 *  PATCH  /api/kafka/topics/{nombre}/configs            cambia configuraciones (incrementalAlterConfigs SET)
 *  POST   /api/kafka/topics/{nombre}/particiones        aumenta las particiones (nunca las reduce)
 *  DELETE /api/kafka/topics/{nombre}                    elimina un topico (los internos "__" estan protegidos)
 *  GET    /api/kafka/consumer-groups                    grupos de consumidores y su estado
 *  GET    /api/kafka/consumer-groups/{groupId}          miembros, offsets por particion y lag
 *  POST   /api/kafka/consumer-groups/{groupId}/offsets  mueve offsets a EARLIEST o LATEST (grupo detenido)
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping("/cluster")
    public ClusterResponse cluster() {
        return service.describirCluster();
    }

    @GetMapping("/topics")
    public List<TopicoResponse> listarTopicos(@RequestParam(defaultValue = "false") boolean internos) {
        return service.listarTopicos(internos);
    }

    @GetMapping("/topics/{nombre}")
    public TopicoDetalleResponse obtenerTopico(
            @PathVariable @Pattern(regexp = Nombres.PATRON_TOPICO_CONSULTA, message = Nombres.MENSAJE_TOPICO_CONSULTA) String nombre) {
        return service.obtenerTopico(nombre);
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public TopicoCreadoResponse crearTopico(@Valid @RequestBody CrearTopicoRequest request) {
        return service.crearTopico(request);
    }

    @PatchMapping("/topics/{nombre}/configs")
    public TopicoDetalleResponse actualizarConfigs(
            @PathVariable @Pattern(regexp = Nombres.PATRON_TOPICO, message = Nombres.MENSAJE_TOPICO) String nombre,
            @Valid @RequestBody ActualizarConfigsRequest request) {
        return service.actualizarConfigs(nombre, request.configs());
    }

    @PostMapping("/topics/{nombre}/particiones")
    public TopicoResponse aumentarParticiones(
            @PathVariable @Pattern(regexp = Nombres.PATRON_TOPICO, message = Nombres.MENSAJE_TOPICO) String nombre,
            @Valid @RequestBody AumentarParticionesRequest request) {
        return service.aumentarParticiones(nombre, request.total());
    }

    @DeleteMapping("/topics/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarTopico(
            @PathVariable @Pattern(regexp = Nombres.PATRON_TOPICO, message = Nombres.MENSAJE_TOPICO) String nombre) {
        service.eliminarTopico(nombre);
    }

    @GetMapping("/consumer-groups")
    public List<GrupoResponse> listarGrupos() {
        return service.listarGrupos();
    }

    @GetMapping("/consumer-groups/{groupId}")
    public GrupoDetalleResponse obtenerGrupo(
            @PathVariable @Pattern(regexp = Nombres.PATRON_GRUPO, message = Nombres.MENSAJE_GRUPO) String groupId) {
        return service.obtenerGrupo(groupId);
    }

    @PostMapping("/consumer-groups/{groupId}/offsets")
    public ResetOffsetsResponse resetearOffsets(
            @PathVariable @Pattern(regexp = Nombres.PATRON_GRUPO, message = Nombres.MENSAJE_GRUPO) String groupId,
            @Valid @RequestBody ResetOffsetsRequest request) {
        return service.resetearOffsets(groupId, request);
    }
}
