package ar.edu.utn.tup.solicitudes.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.tup.solicitudes.persistencia.EstudianteRepository;
import ar.edu.utn.tup.solicitudes.persistencia.ResponsableRepository;
import ar.edu.utn.tup.solicitudes.web.dto.EstudianteResponse;
import ar.edu.utn.tup.solicitudes.web.dto.ResponsableResponse;

@RestController
public class SoporteController {

    private final EstudianteRepository estudianteRepository;
    private final ResponsableRepository responsableRepository;
    private final SolicitudMapper mapper;

    public SoporteController(EstudianteRepository estudianteRepository,
            ResponsableRepository responsableRepository, SolicitudMapper mapper) {
        this.estudianteRepository = estudianteRepository;
        this.responsableRepository = responsableRepository;
        this.mapper = mapper;
    }

    @GetMapping("/api/estudiantes")
    public List<EstudianteResponse> estudiantes() {
        return estudianteRepository.findAll().stream().map(mapper::aResponse).toList();
    }

    @GetMapping("/api/responsables")
    public List<ResponsableResponse> responsables() {
        return responsableRepository.findAll().stream().map(mapper::aResponse).toList();
    }
}
