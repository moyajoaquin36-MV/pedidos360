package cl.duocuc.pedidos360.reportes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Panel de operaciones: consume pedidos-eventos e inventario-movimientos y expone metricas. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsReportesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsReportesApplication.class, args);
	}

}
