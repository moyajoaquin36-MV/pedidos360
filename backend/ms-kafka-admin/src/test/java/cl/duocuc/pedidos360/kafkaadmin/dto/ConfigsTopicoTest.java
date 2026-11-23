package cl.duocuc.pedidos360.kafkaadmin.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigsTopicoTest {

    @Test
    void configsDelContratoSonValidas() {
        assertThat(ConfigsTopico.validar(Map.of(
                "cleanup.policy", "delete", "retention.ms", "604800000", "min.insync.replicas", "2"))).isEmpty();
        assertThat(ConfigsTopico.validar(Map.of("cleanup.policy", "compact,delete", "retention.ms", "-1",
                "retention.bytes", "-1", "segment.ms", "60000", "max.message.bytes", "1024"))).isEmpty();
        assertThat(ConfigsTopico.validar(null)).isEmpty();
    }

    @Test
    void configNoPermitidaSeRechaza() {
        assertThat(ConfigsTopico.validar(Map.of("unclean.leader.election.enable", "true")))
                .singleElement().asString().contains("no esta permitida");
    }

    @Test
    void valoresFueraDeRangoSeRechazan() {
        assertThat(ConfigsTopico.validar("retention.ms", "1000")).contains("60000");
        assertThat(ConfigsTopico.validar("retention.ms", "abc")).isNotNull();
        assertThat(ConfigsTopico.validar("retention.bytes", "0")).isNotNull();
        assertThat(ConfigsTopico.validar("cleanup.policy", "borrar")).isNotNull();
        assertThat(ConfigsTopico.validar("min.insync.replicas", "4")).isNotNull();
        assertThat(ConfigsTopico.validar("min.insync.replicas", "0")).isNotNull();
        assertThat(ConfigsTopico.validar("segment.ms", "59999")).isNotNull();
        assertThat(ConfigsTopico.validar("max.message.bytes", "100")).isNotNull();
        assertThat(ConfigsTopico.validar("max.message.bytes", "99999999999")).isNotNull();
    }

    @Test
    void valorNuloSeRechaza() {
        Map<String, String> configs = new HashMap<>();
        configs.put("retention.ms", null);
        assertThat(ConfigsTopico.validar(configs)).singleElement().asString().contains("debe tener un valor");
    }
}
