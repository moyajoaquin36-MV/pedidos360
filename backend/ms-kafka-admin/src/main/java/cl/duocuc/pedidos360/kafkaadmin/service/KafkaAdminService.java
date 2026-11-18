package cl.duocuc.pedidos360.kafkaadmin.service;

import cl.duocuc.pedidos360.kafkaadmin.config.KafkaAdminProperties;
import cl.duocuc.pedidos360.kafkaadmin.dto.ClusterResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.ConfigResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.ConfigsTopico;
import cl.duocuc.pedidos360.kafkaadmin.dto.CrearTopicoRequest;
import cl.duocuc.pedidos360.kafkaadmin.dto.EstrategiaReset;
import cl.duocuc.pedidos360.kafkaadmin.dto.GrupoDetalleResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.GrupoDetalleResponse.Miembro;
import cl.duocuc.pedidos360.kafkaadmin.dto.GrupoDetalleResponse.OffsetParticion;
import cl.duocuc.pedidos360.kafkaadmin.dto.GrupoResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.ResetOffsetsRequest;
import cl.duocuc.pedidos360.kafkaadmin.dto.ResetOffsetsResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.ResetOffsetsResponse.ParticionOffset;
import cl.duocuc.pedidos360.kafkaadmin.dto.TopicoCreadoResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.TopicoDetalleResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.TopicoResponse;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.GroupListing;
import org.apache.kafka.clients.admin.ListGroupsOptions;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Toda la administracion de Kafka pasa por aqui (AdminClient). El controller
 * solo llama a estos metodos y no conoce las clases de kafka-clients.
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    /** Prefijo de los topicos internos de Kafka (ej. __consumer_offsets). */
    private static final String PREFIJO_INTERNO = "__";
    private static final Comparator<TopicPartition> POR_TOPICO_Y_PARTICION =
            Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition);

    private final ObjectProvider<AdminClient> adminClient;
    private final KafkaAdminProperties props;

    public KafkaAdminService(ObjectProvider<AdminClient> adminClient, KafkaAdminProperties props) {
        this.adminClient = adminClient;
        this.props = props;
    }

    // ---- Cluster ----

    public ClusterResponse describirCluster() {
        DescribeClusterResult cluster = admin().describeCluster();
        Node controlador = esperar(cluster.controller());
        List<ClusterResponse.Broker> brokers = esperar(cluster.nodes()).stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(KafkaAdminService::broker)
                .toList();
        return new ClusterResponse(esperar(cluster.clusterId()),
                controlador == null || controlador.isEmpty() ? null : broker(controlador), brokers);
    }

    // ---- Topicos ----

    /** Los topicos internos (prefijo "__") solo se listan con internos = true. */
    public List<TopicoResponse> listarTopicos(boolean internos) {
        Set<String> nombres = esperar(admin().listTopics(new ListTopicsOptions().listInternal(true)).names()).stream()
                .filter(nombre -> internos || !nombre.startsWith(PREFIJO_INTERNO))
                .collect(Collectors.toSet());
        if (nombres.isEmpty()) {
            return List.of();
        }
        return esperar(admin().describeTopics(nombres).allTopicNames()).values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .map(topico -> new TopicoResponse(topico.name(), topico.partitions().size(), factorReplicacion(topico)))
                .toList();
    }

    /** Lider, replicas e ISR de cada particion y las configuraciones de ConfigsTopico.PERMITIDAS. */
    public TopicoDetalleResponse obtenerTopico(String nombre) {
        TopicDescription topico = describirTopico(nombre);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = esperar(admin().describeConfigs(List.of(recurso)).values().get(recurso));
        List<TopicoDetalleResponse.Particion> particiones = topico.partitions().stream()
                .map(p -> new TopicoDetalleResponse.Particion(p.partition(),
                        p.leader() == null || p.leader().isEmpty() ? null : p.leader().id(),
                        ids(p.replicas()), ids(p.isr())))
                .toList();
        return new TopicoDetalleResponse(topico.name(), topico.isInternal(), factorReplicacion(topico),
                particiones, configsRelevantes(config));
    }

    /**
     * Crea el topico y responde con lo que confirmo Kafka en la misma llamada:
     * un describe inmediato podria no verlo aun en todos los brokers.
     */
    public TopicoCreadoResponse crearTopico(CrearTopicoRequest request) {
        String nombre = request.nombre();
        NewTopic topico = new NewTopic(nombre, request.particiones(), request.replicas().shortValue())
                .configs(request.configsOVacias());
        CreateTopicsResult resultado = admin().createTopics(List.of(topico));
        esperar(resultado.all());
        log.info("Topico creado: {} ({} particiones, {} replicas, configs {})",
                nombre, request.particiones(), request.replicas(), request.configsOVacias());
        return new TopicoCreadoResponse(nombre, esperar(resultado.numPartitions(nombre)),
                esperar(resultado.replicationFactor(nombre)), configsRelevantes(esperar(resultado.config(nombre))));
    }

    /** Fija (SET) solo las configuraciones indicadas; las demas quedan como estaban. */
    public TopicoDetalleResponse actualizarConfigs(String nombre, Map<String, String> configs) {
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Collection<AlterConfigOp> cambios = configs.entrySet().stream()
                .map(config -> new AlterConfigOp(new ConfigEntry(config.getKey(), config.getValue()), AlterConfigOp.OpType.SET))
                .toList();
        esperar(admin().incrementalAlterConfigs(Map.of(recurso, cambios)).all());
        log.info("Configuraciones actualizadas en {}: {}", nombre, configs);
        return obtenerTopico(nombre);
    }

    /**
     * Kafka solo permite aumentar particiones. Ojo: cambia la particion a la que
     * va cada clave, asi que el orden por clave solo se garantiza para eventos nuevos.
     */
    public TopicoResponse aumentarParticiones(String nombre, int total) {
        TopicDescription topico = describirTopico(nombre);
        int actuales = topico.partitions().size();
        if (total <= actuales) {
            throw new OperacionInvalidaException("El topico '" + nombre + "' ya tiene " + actuales
                    + " particiones: solo se pueden aumentar (Kafka no permite reducirlas)");
        }
        esperar(admin().createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all());
        log.info("Particiones de {}: {} -> {}", nombre, actuales, total);
        return new TopicoResponse(nombre, total, factorReplicacion(topico));
    }

    public void eliminarTopico(String nombre) {
        esperar(admin().deleteTopics(List.of(nombre)).all());
        log.info("Topico eliminado: {}", nombre);
    }

    // ---- Grupos de consumidores ----

    public List<GrupoResponse> listarGrupos() {
        return esperar(admin().listGroups(ListGroupsOptions.forConsumerGroups()).all()).stream()
                .sorted(Comparator.comparing(GroupListing::groupId))
                .map(grupo -> new GrupoResponse(grupo.groupId(),
                        grupo.groupState().map(Enum::name).orElse(GroupState.UNKNOWN.name())))
                .toList();
    }

    /** Miembros con sus particiones y, por particion, offset del grupo, end offset y lag. */
    public GrupoDetalleResponse obtenerGrupo(String groupId) {
        ConsumerGroupDescription grupo = describirGrupo(groupId);
        Map<TopicPartition, OffsetAndMetadata> confirmados =
                esperar(admin().listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());

        // Particiones con offset confirmado o asignadas a un miembro (aunque aun no confirme)
        Set<TopicPartition> particiones = new HashSet<>(confirmados.keySet());
        grupo.members().forEach(miembro -> particiones.addAll(miembro.assignment().topicPartitions()));
        Map<TopicPartition, Long> finales = endOffsets(particiones);

        List<OffsetParticion> offsets = particiones.stream()
                .sorted(POR_TOPICO_Y_PARTICION)
                .map(tp -> {
                    OffsetAndMetadata confirmado = confirmados.get(tp);
                    Long offsetGrupo = confirmado == null ? null : confirmado.offset();
                    Long fin = finales.get(tp);
                    Long lag = offsetGrupo == null || fin == null ? null : Math.max(0L, fin - offsetGrupo);
                    return new OffsetParticion(tp.topic(), tp.partition(), offsetGrupo, fin, lag);
                })
                .toList();
        long lagTotal = offsets.stream().map(OffsetParticion::lag).filter(Objects::nonNull).mapToLong(Long::longValue).sum();

        List<Miembro> miembros = grupo.members().stream()
                .sorted(Comparator.comparing(MemberDescription::clientId))
                .map(miembro -> new Miembro(miembro.clientId(), miembro.host(),
                        miembro.assignment().topicPartitions().stream().sorted(POR_TOPICO_Y_PARTICION).map(TopicPartition::toString).toList()))
                .toList();
        return new GrupoDetalleResponse(groupId, grupo.groupState().name(), miembros, offsets, lagTotal);
    }

    /**
     * Mueve los offsets del grupo en todas las particiones del topico (EARLIEST
     * para reprocesar eventos). Solo con el grupo vacio: con consumidores
     * conectados Kafka lo rechaza y ellos volverian a confirmar sus offsets.
     */
    public ResetOffsetsResponse resetearOffsets(String groupId, ResetOffsetsRequest request) {
        ConsumerGroupDescription grupo = describirGrupo(groupId);
        if (grupo.groupState() != GroupState.EMPTY || !grupo.members().isEmpty()) {
            throw new GrupoActivoException("El grupo '" + groupId + "' esta activo (estado " + grupo.groupState().name()
                    + ", " + grupo.members().size() + " consumidores): detenga sus consumidores y vuelva a intentar");
        }
        TopicDescription topico = describirTopico(request.topico());
        OffsetSpec destino = request.estrategia() == EstrategiaReset.EARLIEST ? OffsetSpec.earliest() : OffsetSpec.latest();
        Map<TopicPartition, OffsetSpec> consulta = topico.partitions().stream()
                .collect(Collectors.toMap(p -> new TopicPartition(topico.name(), p.partition()), p -> destino));
        Map<TopicPartition, OffsetAndMetadata> nuevos = esperar(admin().listOffsets(consulta).all()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new OffsetAndMetadata(e.getValue().offset())));
        esperar(admin().alterConsumerGroupOffsets(groupId, nuevos).all());

        List<ParticionOffset> offsets = nuevos.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(POR_TOPICO_Y_PARTICION))
                .map(e -> new ParticionOffset(e.getKey().partition(), e.getValue().offset()))
                .toList();
        log.info("Offsets del grupo {} en {} movidos a {}: {}", groupId, topico.name(), request.estrategia(), offsets);
        return new ResetOffsetsResponse(groupId, topico.name(), request.estrategia(), offsets);
    }

    // ---- Apoyo ----

    /** Lanza UnknownTopicOrPartitionException (404) si el topico no existe. */
    private TopicDescription describirTopico(String nombre) {
        return esperar(admin().describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
    }

    /** Para un grupo inexistente Kafka responde estado DEAD (o GroupIdNotFoundException en brokers nuevos). */
    private ConsumerGroupDescription describirGrupo(String groupId) {
        ConsumerGroupDescription grupo = esperar(admin().describeConsumerGroups(List.of(groupId)).describedGroups().get(groupId));
        if (grupo.groupState() == GroupState.DEAD) {
            throw new RecursoNoEncontradoException("El grupo de consumidores '" + groupId + "' no existe");
        }
        return grupo;
    }

    /** End offset (siguiente offset a escribir) por particion; omite las de topicos ya borrados. */
    private Map<TopicPartition, Long> endOffsets(Set<TopicPartition> particiones) {
        Set<String> existentes = esperar(admin().listTopics(new ListTopicsOptions().listInternal(true)).names());
        Map<TopicPartition, OffsetSpec> consulta = particiones.stream()
                .filter(tp -> existentes.contains(tp.topic()))
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
        if (consulta.isEmpty()) {
            return Map.of();
        }
        return esperar(admin().listOffsets(consulta).all()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().offset()));
    }

    private static List<ConfigResponse> configsRelevantes(Config config) {
        return ConfigsTopico.PERMITIDAS.stream()
                .map(config::get)
                .filter(Objects::nonNull)
                .map(entrada -> {
                    ConfigEntry.ConfigSource origen = entrada.source() == null ? ConfigEntry.ConfigSource.UNKNOWN : entrada.source();
                    return new ConfigResponse(entrada.name(), entrada.value(),
                            origen != ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG, origen.name());
                })
                .toList();
    }

    private static int factorReplicacion(TopicDescription topico) {
        return topico.partitions().isEmpty() ? 0 : topico.partitions().get(0).replicas().size();
    }

    private static List<Integer> ids(List<Node> nodos) {
        return nodos.stream().map(Node::id).toList();
    }

    private static ClusterResponse.Broker broker(Node nodo) {
        return new ClusterResponse.Broker(nodo.id(), nodo.host(), nodo.port());
    }

    /** El AdminClient es @Lazy: se crea (sin conectarse aun) en la primera llamada. */
    private AdminClient admin() {
        try {
            return adminClient.getObject();
        } catch (BeansException e) {
            // ej. ningun host de bootstrap-servers resuelve todavia (cluster sin levantar)
            throw new KafkaNoDisponibleException("No se pudo crear el cliente de Kafka: "
                    + NestedExceptionUtils.getMostSpecificCause(e).getMessage(), e);
        }
    }

    /**
     * Espera el resultado de una operacion del AdminClient. Si Kafka la rechaza
     * llega una ExecutionException: se relanza su causa (TopicExistsException,
     * UnknownTopicOrPartitionException, ...) y GlobalExceptionHandler responde
     * segun el error real.
     */
    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(props.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw new KafkaNoDisponibleException("Kafka no pudo completar la operacion", e);
        } catch (TimeoutException e) {
            throw new KafkaNoDisponibleException("Kafka no respondio en " + props.timeout().toSeconds() + " s", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Se interrumpio la espera de la respuesta de Kafka", e);
        }
    }
}
