package ar.edu.utn.tup.solicitudes.web;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.utn.tup.solicitudes.aplicacion.NuevaSolicitud;
import ar.edu.utn.tup.solicitudes.aplicacion.ReporteResumen;
import ar.edu.utn.tup.solicitudes.dominio.EstadoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.Estudiante;
import ar.edu.utn.tup.solicitudes.dominio.Prioridad;
import ar.edu.utn.tup.solicitudes.dominio.Responsable;
import ar.edu.utn.tup.solicitudes.dominio.Solicitud;
import ar.edu.utn.tup.solicitudes.dominio.TipoSolicitud;
import ar.edu.utn.tup.solicitudes.web.dto.RegistrarSolicitudRequest;

@Component
public class SolicitudMapper {

    public NuevaSolicitud aNuevaSolicitud(RegistrarSolicitudRequest request) {
        validarDescripcion(request.descripcion());
        TipoSolicitud tipo = aTipo(request.tipo());
        Prioridad prioridad = request.prioridad() == null ? null : aPrioridad(request.prioridad());
        return new NuevaSolicitud(request.legajo(), tipo, request.descripcion(), prioridad);
    }

    public TipoSolicitud aTipo(String texto) {
        return TipoSolicitud.desde(texto)
                .orElseThrow(() -> new RuntimeException("400|Tipo de solicitud inválido: " + texto));
    }

    public EstadoSolicitud aEstado(String texto) {
        return EstadoSolicitud.desde(texto)
                .orElseThrow(() -> new RuntimeException("400|Estado inválido: " + texto));
    }

    public Prioridad aPrioridad(String texto) {
        return Prioridad.desde(texto)
                .orElseThrow(() -> new RuntimeException("400|Prioridad inválida: " + texto));
    }

    public Map<String, Object> aMap(Solicitud s) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", s.getId());
        res.put("tipo", s.getTipo().name());
        res.put("descripcion", s.getDescripcion());
        res.put("prioridad", s.getPrioridad().name());
        res.put("estado", s.getEstado().name());
        res.put("fechaCreacion", s.getFechaCreacion());
        res.put("fechaVencimiento", s.getFechaVencimiento());
        res.put("fechaActualizacion", s.getFechaActualizacion());
        res.put("vencida", s.estaVencida(LocalDateTime.now()));
        res.put("estudiante", aMapEstudiante(s.getEstudiante()));
        res.put("responsable", aMapResponsable(s.getResponsable()));
        return res;
    }

    public List<Map<String, Object>> aMap(List<Solicitud> solicitudes) {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Solicitud s : solicitudes) {
            salida.add(aMap(s));
        }
        return salida;
    }

    public Map<String, Object> aMap(ReporteResumen resumen) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total", resumen.total());
        res.put("porEstado", nombresConConteo(resumen.porEstado()));
        res.put("porTipo", nombresConConteo(resumen.porTipo()));
        res.put("vencidas", resumen.vencidas());
        return res;
    }

    private Map<String, Integer> nombresConConteo(Map<? extends Enum<?>, Integer> conteos) {
        Map<String, Integer> nombres = new LinkedHashMap<>();
        conteos.forEach((constante, cantidad) -> nombres.put(constante.name(), cantidad));
        return nombres;
    }

    private Map<String, Object> aMapEstudiante(Estudiante est) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("legajo", est.getLegajo());
        e.put("nombre", est.getNombre());
        return e;
    }

    private Map<String, Object> aMapResponsable(Responsable resp) {
        if (resp == null) {
            return null;
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", resp.getId());
        r.put("nombre", resp.getNombre());
        r.put("area", resp.getArea());
        return r;
    }

    private void validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.length() < 10 || descripcion.length() > 500) {
            throw new RuntimeException("400|La descripción debe tener entre 10 y 500 caracteres");
        }
    }
}
