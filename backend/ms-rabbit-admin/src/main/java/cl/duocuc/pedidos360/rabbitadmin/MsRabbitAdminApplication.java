package cl.duocuc.pedidos360.rabbitadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Control plane de RabbitMQ: administra colas, exchanges y bindings por REST. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsRabbitAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsRabbitAdminApplication.class, args);
	}

}
