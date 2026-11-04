package ar.edu.utn.tup.solicitudes.aplicacion;

import ar.edu.utn.tup.solicitudes.dominio.EstadoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.Prioridad;
import ar.edu.utn.tup.solicitudes.dominio.Solicitud;
import ar.edu.utn.tup.solicitudes.dominio.TipoSolicitud;

public record FiltroSolicitudes(EstadoSolicitud estado, TipoSolicitud tipo, Prioridad prioridad, String legajo) {

    public boolean coincideCon(Solicitud solicitud) {
        return (estado == null || solicitud.getEstado() == estado)
                && (tipo == null || solicitud.getTipo() == tipo)
                && (prioridad == null || solicitud.getPrioridad() == prioridad)
                && (legajo == null || solicitud.getEstudiante().getLegajo().equals(legajo));
    }
}
