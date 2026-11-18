package cl.duocuc.pedidos360.auditoria.controller;

import cl.duocuc.pedidos360.auditoria.auditoria.AuditoriaService;
import cl.duocuc.pedidos360.auditoria.dto.EventoAuditoriaResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/auditoria/eventos?pedidoId=&productoId=&tipo=&limite=
 * Historial de acciones, la mas reciente primero (solo ADMIN_GENERAL).
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/eventos")
    public List<EventoAuditoriaResponse> eventos(
            @RequestParam(required = false) Long pedidoId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "limite debe ser al menos 1")
            @Max(value = 200, message = "limite no puede superar 200") int limite) {
        return auditoriaService.buscar(pedidoId, productoId, tipo, limite).stream()
                .map(EventoAuditoriaResponse::desde)
                .toList();
    }
}
