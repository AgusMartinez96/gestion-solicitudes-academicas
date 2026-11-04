package ar.edu.utn.tup.solicitudes.web.dto;

import java.util.Map;

import ar.edu.utn.tup.solicitudes.dominio.EstadoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.TipoSolicitud;

public record ReporteResumenResponse(
        int total,
        Map<EstadoSolicitud, Integer> porEstado,
        Map<TipoSolicitud, Integer> porTipo,
        int vencidas) {
}
