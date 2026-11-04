package ar.edu.utn.tup.solicitudes.web.dto;

import java.time.LocalDateTime;

import ar.edu.utn.tup.solicitudes.dominio.EstadoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.Prioridad;
import ar.edu.utn.tup.solicitudes.dominio.TipoSolicitud;

public record SolicitudResponse(
        Long id,
        TipoSolicitud tipo,
        String descripcion,
        Prioridad prioridad,
        EstadoSolicitud estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaVencimiento,
        LocalDateTime fechaActualizacion,
        boolean vencida,
        EstudianteResumen estudiante,
        ResponsableResumen responsable) {
}
