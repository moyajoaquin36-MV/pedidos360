package cl.duocuc.pedidos360.kafkaadmin.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * AdminClient de Kafka: gestiona topicos, configuraciones y grupos en tiempo
 * de ejecucion. Se arma con la misma configuracion de spring.kafka que Spring
 * Boot entrega al KafkaAdmin (bootstrap-servers y spring.kafka.admin.properties
 * con los timeouts).
 *
 * Es @Lazy: no se crea ni se conecta al arrancar, sino en la primera llamada a
 * la API, asi el servicio levanta aunque el cluster aun no este arriba.
 * Spring lo cierra al apagar (close espera como maximo default.api.timeout.ms).
 */
@Configuration
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    @Lazy
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }
}
