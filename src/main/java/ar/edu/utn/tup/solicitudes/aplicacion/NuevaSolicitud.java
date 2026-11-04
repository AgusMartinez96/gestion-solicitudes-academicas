package ar.edu.utn.tup.solicitudes.aplicacion;

import ar.edu.utn.tup.solicitudes.model.Prioridad;
import ar.edu.utn.tup.solicitudes.model.TipoSolicitud;

public record NuevaSolicitud(String legajo, TipoSolicitud tipo, String descripcion, Prioridad prioridad) {

    public Prioridad prioridadEfectiva() {
        if (prioridad != null) {
            return prioridad;
        }
        return tipo.prioridadPorDefecto();
    }

    public int plazoDias() {
        return prioridadEfectiva().ajustarPlazo(tipo.plazoDias());
    }
}
