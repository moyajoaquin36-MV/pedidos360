package cl.duocuc.pedidos360.kafkaadmin.service;

import cl.duocuc.pedidos360.kafkaadmin.config.KafkaAdminProperties;
import cl.duocuc.pedidos360.kafkaadmin.dto.EstrategiaReset;
import cl.duocuc.pedidos360.kafkaadmin.dto.GrupoDetalleResponse;
import cl.duocuc.pedidos360.kafkaadmin.dto.ResetOffsetsRequest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.GroupType;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaAdminServiceTest {

    private static final Node BROKER = new Node(1, "kafka-1", 29092);

    private final AdminClient admin = mock(AdminClient.class);
    private KafkaAdminService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<AdminClient> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(admin);
        service = new KafkaAdminService(provider, new KafkaAdminProperties(Duration.ofSeconds(1)));
    }

    @Test
    void listarTopicosOcultaLosInternosPorDefecto() {
        existenTopicos("pedidos-eventos", "__consumer_offsets");
        DescribeTopicsResult descripcion = mock(DescribeTopicsResult.class);
        when(descripcion.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of("pedidos-eventos", topico("pedidos-eventos", 3))));
        when(admin.describeTopics(anyCollection())).thenReturn(descripcion);

        assertThat(service.listarTopicos(false)).singleElement()
                .satisfies(t -> assertThat(t.nombre()).isEqualTo("pedidos-eventos"))
                .satisfies(t -> assertThat(t.particiones()).isEqualTo(3));
        verify(admin).describeTopics(Set.of("pedidos-eventos"));
    }

    @Test
    void errorDeKafkaSeDesenvuelveDelFuturo() {
        KafkaFutureImpl<Void> fallido = new KafkaFutureImpl<>();
        fallido.completeExceptionally(new UnknownTopicOrPartitionException("no existe"));
        DeleteTopicsResult resultado = mock(DeleteTopicsResult.class);
        when(resultado.all()).thenReturn(fallido);
        when(admin.deleteTopics(anyCollection())).thenReturn(resultado);

        assertThatThrownBy(() -> service.eliminarTopico("no-existe")).isInstanceOf(UnknownTopicOrPartitionException.class);
    }

    @Test
    void futuroQueNoTerminaDa503() {
        DeleteTopicsResult resultado = mock(DeleteTopicsResult.class);
        when(resultado.all()).thenReturn(new KafkaFutureImpl<>());
        when(admin.deleteTopics(anyCollection())).thenReturn(resultado);

        assertThatThrownBy(() -> service.eliminarTopico("lento")).isInstanceOf(KafkaNoDisponibleException.class);
    }

    @Test
    void noSePuedenReducirParticiones() {
        describeTopico(topico("pedidos-eventos", 3));

        assertThatThrownBy(() -> service.aumentarParticiones("pedidos-eventos", 3))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("ya tiene 3 particiones");
        verify(admin, never()).createPartitions(anyMap());
    }

    @Test
    void resetDeOffsetsConGrupoActivoDa409() {
        describeGrupo(grupo("grupo-reporteria", GroupState.STABLE, miembro(new TopicPartition("pedidos-eventos", 0))));

        assertThatThrownBy(() -> service.resetearOffsets("grupo-reporteria",
                new ResetOffsetsRequest("pedidos-eventos", EstrategiaReset.EARLIEST)))
                .isInstanceOf(GrupoActivoException.class)
                .hasMessageContaining("STABLE");
        verify(admin, never()).alterConsumerGroupOffsets(anyString(), anyMap());
    }

    @Test
    void grupoInexistenteDa404() {
        describeGrupo(grupo("no-existe", GroupState.DEAD));

        assertThatThrownBy(() -> service.obtenerGrupo("no-existe")).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void detalleDeGrupoCalculaElLag() {
        TopicPartition p0 = new TopicPartition("pedidos-eventos", 0);
        TopicPartition p1 = new TopicPartition("pedidos-eventos", 1);
        describeGrupo(grupo("grupo-reporteria", GroupState.STABLE, miembro(p0, p1)));
        ListConsumerGroupOffsetsResult confirmados = mock(ListConsumerGroupOffsetsResult.class);
        when(confirmados.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(Map.of(p0, new OffsetAndMetadata(5))));
        when(admin.listConsumerGroupOffsets("grupo-reporteria")).thenReturn(confirmados);
        existenTopicos("pedidos-eventos");
        ListOffsetsResult finales = mock(ListOffsetsResult.class);
        when(finales.all()).thenReturn(KafkaFuture.completedFuture(Map.of(
                p0, new ListOffsetsResult.ListOffsetsResultInfo(12, -1, Optional.empty()),
                p1, new ListOffsetsResult.ListOffsetsResultInfo(7, -1, Optional.empty()))));
        when(admin.listOffsets(anyMap())).thenReturn(finales);

        GrupoDetalleResponse detalle = service.obtenerGrupo("grupo-reporteria");

        assertThat(detalle.lagTotal()).isEqualTo(7);
        assertThat(detalle.offsets()).extracting(GrupoDetalleResponse.OffsetParticion::lag).containsExactly(7L, null);
        assertThat(detalle.miembros()).singleElement()
                .satisfies(m -> assertThat(m.particiones()).containsExactly("pedidos-eventos-0", "pedidos-eventos-1"));
    }

    // ---- Apoyo ----

    private void existenTopicos(String... nombres) {
        ListTopicsResult lista = mock(ListTopicsResult.class);
        when(lista.names()).thenReturn(KafkaFuture.completedFuture(Set.of(nombres)));
        when(admin.listTopics(any())).thenReturn(lista);
    }

    private void describeTopico(TopicDescription topico) {
        DescribeTopicsResult descripcion = mock(DescribeTopicsResult.class);
        when(descripcion.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of(topico.name(), topico)));
        when(admin.describeTopics(anyCollection())).thenReturn(descripcion);
    }

    private void describeGrupo(ConsumerGroupDescription grupo) {
        DescribeConsumerGroupsResult resultado = mock(DescribeConsumerGroupsResult.class);
        when(resultado.describedGroups()).thenReturn(Map.of(grupo.groupId(), KafkaFuture.completedFuture(grupo)));
        when(admin.describeConsumerGroups(anyCollection())).thenReturn(resultado);
    }

    private static TopicDescription topico(String nombre, int particiones) {
        return new TopicDescription(nombre, false, IntStream.range(0, particiones)
                .mapToObj(i -> new TopicPartitionInfo(i, BROKER, List.of(BROKER, BROKER, BROKER), List.of(BROKER)))
                .toList());
    }

    private static ConsumerGroupDescription grupo(String groupId, GroupState estado, MemberDescription... miembros) {
        return new ConsumerGroupDescription(groupId, false, List.of(miembros), "roundrobin", GroupType.CLASSIC,
                estado, BROKER, Set.of(), Optional.empty(), Optional.empty());
    }

    private static MemberDescription miembro(TopicPartition... particiones) {
        return new MemberDescription("consumer-1", Optional.empty(), "ms-reportes-0", "/10.0.0.5",
                new MemberAssignment(Set.of(particiones)), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
