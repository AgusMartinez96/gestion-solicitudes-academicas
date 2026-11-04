package ar.edu.utn.tup.solicitudes.aplicacion;

import ar.edu.utn.tup.solicitudes.dominio.Solicitud;

public record SolicitudDetalle(Solicitud solicitud, boolean vencida) {
}
