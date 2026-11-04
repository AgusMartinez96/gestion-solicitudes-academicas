package ar.edu.utn.tup.solicitudes.web;

import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.utn.tup.solicitudes.aplicacion.PedidoRegistro;
import ar.edu.utn.tup.solicitudes.aplicacion.ReporteResumen;
import ar.edu.utn.tup.solicitudes.aplicacion.SolicitudDetalle;
import ar.edu.utn.tup.solicitudes.dominio.Estudiante;
import ar.edu.utn.tup.solicitudes.dominio.Responsable;
import ar.edu.utn.tup.solicitudes.dominio.Solicitud;
import ar.edu.utn.tup.solicitudes.web.dto.EstudianteResponse;
import ar.edu.utn.tup.solicitudes.web.dto.EstudianteResumen;
import ar.edu.utn.tup.solicitudes.web.dto.RegistrarSolicitudRequest;
import ar.edu.utn.tup.solicitudes.web.dto.ReporteResumenResponse;
import ar.edu.utn.tup.solicitudes.web.dto.ResponsableResponse;
import ar.edu.utn.tup.solicitudes.web.dto.ResponsableResumen;
import ar.edu.utn.tup.solicitudes.web.dto.SolicitudResponse;

@Component
public class SolicitudMapper {

    public PedidoRegistro aPedido(RegistrarSolicitudRequest request) {
        return new PedidoRegistro(request.legajo(), request.tipo(), request.descripcion(), request.prioridad());
    }

    public SolicitudResponse aResponse(SolicitudDetalle detalle) {
        Solicitud solicitud = detalle.solicitud();
        return new SolicitudResponse(
                solicitud.getId(),
                solicitud.getTipo(),
                solicitud.getDescripcion(),
                solicitud.getPrioridad(),
                solicitud.getEstado(),
                solicitud.getFechaCreacion(),
                solicitud.getFechaVencimiento(),
                solicitud.getFechaActualizacion(),
                detalle.vencida(),
                aResumen(solicitud.getEstudiante()),
                aResumen(solicitud.getResponsable()));
    }

    public List<SolicitudResponse> aResponse(List<SolicitudDetalle> detalles) {
        return detalles.stream().map(this::aResponse).toList();
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
}
