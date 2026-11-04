package ar.edu.utn.tup.solicitudes.aplicacion;

import java.util.Map;

import ar.edu.utn.tup.solicitudes.dominio.EstadoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.TipoSolicitud;

public record ReporteResumen(int total, Map<EstadoSolicitud, Integer> porEstado,
        Map<TipoSolicitud, Integer> porTipo, int vencidas) {
}
