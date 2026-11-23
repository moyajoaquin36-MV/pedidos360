package cl.duocuc.pedidos360.kafkaadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Administrador de Kafka: gestiona topicos, configuraciones y grupos de consumidores por REST. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsKafkaAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsKafkaAdminApplication.class, args);
	}

}
