package cl.duocuc.pedidos360.pedidos.client;

import cl.duocuc.pedidos360.pedidos.controller.ProductosNoDisponibleException;
import cl.duocuc.pedidos360.pedidos.controller.StockInsuficienteException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Llama a ms-productos reenviando el mismo JWT del usuario (misma audience),
 * de modo que ms-productos aplique su propia validacion y autorizacion.
 */
@Component
public class ProductosClient {

    public record ItemStock(Long productoId, Integer cantidad) {
    }

    record DescontarStockRequest(List<ItemStock> items) {
    }

    private final RestClient restClient;

    public ProductosClient(@Value("${app.productos.base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void descontarStock(String bearerToken, List<ItemStock> items) {
        try {
            restClient.post()
                    .uri("/api/productos/descuento-stock")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DescontarStockRequest(items))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) {
                Map<?, ?> body = e.getResponseBodyAs(Map.class);
                Object mensaje = body != null ? body.get("mensaje") : null;
                throw new StockInsuficienteException(mensaje != null ? mensaje.toString() : "Stock insuficiente");
            }
            throw new ProductosNoDisponibleException("ms-productos rechazo la solicitud (" + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            throw new ProductosNoDisponibleException("No se pudo contactar a ms-productos");
        }
    }
}
