package cl.duocuc.pedidos360.reportes.controller;

import cl.duocuc.pedidos360.reportes.metricas.MetricasResponse;
import cl.duocuc.pedidos360.reportes.metricas.MetricasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/reportes/metricas: panel de operaciones en tiempo real
 * (ADMIN_GENERAL y OPERADOR_COCINA, ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    private final MetricasService metricasService;

    public ReportesController(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @GetMapping("/metricas")
    public MetricasResponse metricas() {
        return metricasService.obtenerMetricas();
    }
}
