package cl.duocuc.pedidos360.auditoria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Auditoria y trazabilidad: guarda cada evento de pedidos e inventario con fecha, usuario y rol. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsAuditoriaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsAuditoriaApplication.class, args);
	}

}
