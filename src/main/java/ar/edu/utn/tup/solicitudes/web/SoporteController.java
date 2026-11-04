package ar.edu.utn.tup.solicitudes.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.tup.solicitudes.aplicacion.ConsultaService;
import ar.edu.utn.tup.solicitudes.web.dto.EstudianteResponse;
import ar.edu.utn.tup.solicitudes.web.dto.ResponsableResponse;

@RestController
public class SoporteController {

    private final ConsultaService consultaService;
    private final SolicitudMapper mapper;

    public SoporteController(ConsultaService consultaService, SolicitudMapper mapper) {
        this.consultaService = consultaService;
        this.mapper = mapper;
    }

    @GetMapping("/api/estudiantes")
    public List<EstudianteResponse> estudiantes() {
        return consultaService.estudiantes().stream().map(mapper::aResponse).toList();
    }

    @GetMapping("/api/responsables")
    public List<ResponsableResponse> responsables() {
        return consultaService.responsables().stream().map(mapper::aResponse).toList();
    }
}
