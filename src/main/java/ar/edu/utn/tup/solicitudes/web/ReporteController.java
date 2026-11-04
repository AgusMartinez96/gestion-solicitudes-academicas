package ar.edu.utn.tup.solicitudes.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.tup.solicitudes.aplicacion.ReporteService;
import ar.edu.utn.tup.solicitudes.web.dto.ReporteResumenResponse;

@RestController
public class ReporteController {

    private final ReporteService reporteService;
    private final SolicitudMapper mapper;

    public ReporteController(ReporteService reporteService, SolicitudMapper mapper) {
        this.reporteService = reporteService;
        this.mapper = mapper;
    }

    @GetMapping("/api/reportes/resumen")
    public ReporteResumenResponse resumen() {
        return mapper.aResponse(reporteService.resumen());
    }
}
