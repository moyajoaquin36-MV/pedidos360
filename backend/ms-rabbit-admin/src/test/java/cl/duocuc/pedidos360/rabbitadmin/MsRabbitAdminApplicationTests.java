package cl.duocuc.pedidos360.rabbitadmin;

import cl.duocuc.pedidos360.rabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Carga el contexto real (sin mocks): falla si falta algun bean, como RabbitAdmin. */
@SpringBootTest
class MsRabbitAdminApplicationTests {

    @Autowired
    private RabbitAdminService service;

    @Test
    void contextoCargaConElServicioReal() {
        assertThat(service).isNotNull();
    }
}
