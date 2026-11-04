package ar.edu.utn.tup.solicitudes.aplicacion;

import ar.edu.utn.tup.solicitudes.dominio.Solicitud;

public interface Notificador {

    void notificarCierre(Solicitud solicitud);
}
