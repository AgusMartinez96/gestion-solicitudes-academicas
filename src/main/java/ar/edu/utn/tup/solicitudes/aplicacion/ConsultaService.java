package ar.edu.utn.tup.solicitudes.aplicacion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.tup.solicitudes.dominio.Estudiante;
import ar.edu.utn.tup.solicitudes.dominio.Responsable;
import ar.edu.utn.tup.solicitudes.persistencia.EstudianteRepository;
import ar.edu.utn.tup.solicitudes.persistencia.ResponsableRepository;

@Service
@Transactional(readOnly = true)
public class ConsultaService {

    private final EstudianteRepository estudianteRepository;
    private final ResponsableRepository responsableRepository;

    public ConsultaService(EstudianteRepository estudianteRepository,
            ResponsableRepository responsableRepository) {
        this.estudianteRepository = estudianteRepository;
        this.responsableRepository = responsableRepository;
    }

    public List<Estudiante> estudiantes() {
        return estudianteRepository.findAll();
    }

    public List<Responsable> responsables() {
        return responsableRepository.findAll();
    }
}
