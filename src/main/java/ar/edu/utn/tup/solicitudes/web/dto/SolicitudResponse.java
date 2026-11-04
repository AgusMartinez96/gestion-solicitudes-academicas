package ar.edu.utn.tup.solicitudes.web.dto;

import java.time.LocalDateTime;

import ar.edu.utn.tup.solicitudes.dominio.EstadoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.Prioridad;
import ar.edu.utn.tup.solicitudes.dominio.TipoSolicitud;
import io.swagger.v3.oas.annotations.media.Schema;

public record SolicitudResponse(
        Long id,
        TipoSolicitud tipo,
        String descripcion,
        Prioridad prioridad,
        EstadoSolicitud estado,
        @Schema(type = "string", format = "date-time", example = "2026-11-04T10:29:24") LocalDateTime fechaCreacion,
        @Schema(type = "string", format = "date-time", example = "2026-11-04T10:29:24") LocalDateTime fechaVencimiento,
        @Schema(type = "string", format = "date-time", example = "2026-11-04T10:29:24") LocalDateTime fechaActualizacion,
        boolean vencida,
        EstudianteResumen estudiante,
        ResponsableResumen responsable) {
}
