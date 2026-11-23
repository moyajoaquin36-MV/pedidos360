package cl.duocuc.pedidos360.kafkaadmin;

import cl.duocuc.pedidos360.kafkaadmin.service.KafkaAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Carga el contexto real (sin mocks y sin broker): falla si falta algun bean. */
@SpringBootTest
class MsKafkaAdminApplicationTests {

    @Autowired
    private KafkaAdminService service;

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void contextoCargaSinConectarseAKafka() {
        assertThat(service).isNotNull();
        // AdminClient es @Lazy: al arrancar ni se crea ni se conecta al broker
        // (apenas se crea empieza a buscar metadata, por eso no se instancia aqui)
        assertThat(context.getBeanFactory().containsSingleton("adminClient")).isFalse();
        assertThat(context.getBeanFactory().getBeanDefinition("adminClient").isLazyInit()).isTrue();
    }
}
