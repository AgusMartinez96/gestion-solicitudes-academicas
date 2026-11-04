package ar.edu.utn.tup.solicitudes.web;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.utn.tup.solicitudes.aplicacion.NuevaSolicitud;
import ar.edu.utn.tup.solicitudes.aplicacion.ReporteResumen;
import ar.edu.utn.tup.solicitudes.dominio.EstadoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.Estudiante;
import ar.edu.utn.tup.solicitudes.dominio.Prioridad;
import ar.edu.utn.tup.solicitudes.dominio.Responsable;
import ar.edu.utn.tup.solicitudes.dominio.Solicitud;
import ar.edu.utn.tup.solicitudes.dominio.TipoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.excepciones.DatoInvalidoException;
import ar.edu.utn.tup.solicitudes.web.dto.EstudianteResponse;
import ar.edu.utn.tup.solicitudes.web.dto.EstudianteResumen;
import ar.edu.utn.tup.solicitudes.web.dto.RegistrarSolicitudRequest;
import ar.edu.utn.tup.solicitudes.web.dto.ReporteResumenResponse;
import ar.edu.utn.tup.solicitudes.web.dto.ResponsableResponse;
import ar.edu.utn.tup.solicitudes.web.dto.ResponsableResumen;
import ar.edu.utn.tup.solicitudes.web.dto.SolicitudResponse;

@Component
public class SolicitudMapper {

    private static final int LONGITUD_MINIMA_DESCRIPCION = 10;
    private static final int LONGITUD_MAXIMA_DESCRIPCION = 500;

    private final Clock clock;

    public SolicitudMapper(Clock clock) {
        this.clock = clock;
    }

    public NuevaSolicitud aNuevaSolicitud(RegistrarSolicitudRequest request) {
        validarDescripcion(request.descripcion());
        TipoSolicitud tipo = aTipo(request.tipo());
        Prioridad prioridad = request.prioridad() == null ? null : aPrioridad(request.prioridad());
        return new NuevaSolicitud(request.legajo(), tipo, request.descripcion(), prioridad);
    }

    public TipoSolicitud aTipo(String texto) {
        return TipoSolicitud.desde(texto)
                .orElseThrow(() -> new DatoInvalidoException("Tipo de solicitud inválido: " + texto));
    }

    public EstadoSolicitud aEstado(String texto) {
        return EstadoSolicitud.desde(texto)
                .orElseThrow(() -> new DatoInvalidoException("Estado inválido: " + texto));
    }

    public Prioridad aPrioridad(String texto) {
        return Prioridad.desde(texto)
                .orElseThrow(() -> new DatoInvalidoException("Prioridad inválida: " + texto));
    }

    public SolicitudResponse aResponse(Solicitud solicitud) {
        return new SolicitudResponse(
                solicitud.getId(),
                solicitud.getTipo(),
                solicitud.getDescripcion(),
                solicitud.getPrioridad(),
                solicitud.getEstado(),
                solicitud.getFechaCreacion(),
                solicitud.getFechaVencimiento(),
                solicitud.getFechaActualizacion(),
                solicitud.estaVencida(LocalDateTime.now(clock)),
                aResumen(solicitud.getEstudiante()),
                aResumen(solicitud.getResponsable()));
    }

    public List<SolicitudResponse> aResponse(List<Solicitud> solicitudes) {
        return solicitudes.stream().map(this::aResponse).toList();
    }

    public ReporteResumenResponse aResponse(ReporteResumen resumen) {
        return new ReporteResumenResponse(resumen.total(), resumen.porEstado(), resumen.porTipo(),
                resumen.vencidas());
    }

    public EstudianteResponse aResponse(Estudiante estudiante) {
        return new EstudianteResponse(estudiante.getId(), estudiante.getLegajo(), estudiante.getNombre(),
                estudiante.getEmail());
    }

    public ResponsableResponse aResponse(Responsable responsable) {
        return new ResponsableResponse(responsable.getId(), responsable.getNombre(), responsable.getArea(),
                responsable.isActivo());
    }

    private EstudianteResumen aResumen(Estudiante estudiante) {
        return new EstudianteResumen(estudiante.getLegajo(), estudiante.getNombre());
    }

    private ResponsableResumen aResumen(Responsable responsable) {
        if (responsable == null) {
            return null;
        }
        return new ResponsableResumen(responsable.getId(), responsable.getNombre(), responsable.getArea());
    }

    private void validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.length() < LONGITUD_MINIMA_DESCRIPCION
                || descripcion.length() > LONGITUD_MAXIMA_DESCRIPCION) {
            throw new DatoInvalidoException("La descripción debe tener entre " + LONGITUD_MINIMA_DESCRIPCION
                    + " y " + LONGITUD_MAXIMA_DESCRIPCION + " caracteres");
        }
    }
}
