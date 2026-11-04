package ar.edu.utn.tup.solicitudes.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.tup.solicitudes.dominio.Estudiante;
import ar.edu.utn.tup.solicitudes.dominio.Responsable;
import ar.edu.utn.tup.solicitudes.persistencia.EstudianteRepository;
import ar.edu.utn.tup.solicitudes.persistencia.ResponsableRepository;

@RestController
public class SoporteController {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private ResponsableRepository responsableRepository;

    @GetMapping("/api/estudiantes")
    public List<Map<String, Object>> estudiantes() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Estudiante e : estudianteRepository.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("legajo", e.getLegajo());
            m.put("nombre", e.getNombre());
            m.put("email", e.getEmail());
            lista.add(m);
        }
        return lista;
    }

    @GetMapping("/api/responsables")
    public List<Map<String, Object>> responsables() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Responsable r : responsableRepository.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("nombre", r.getNombre());
            m.put("area", r.getArea());
            m.put("activo", r.isActivo());
            lista.add(m);
        }
        return lista;
    }
}
