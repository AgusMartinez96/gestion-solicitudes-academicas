package ar.edu.utn.tup.solicitudes.aplicacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ar.edu.utn.tup.solicitudes.dominio.Solicitud;

@Component
public class NotificadorLog implements Notificador {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorLog.class);

    @Override
    public void notificarCierre(Solicitud solicitud) {
        LOG.info("Notificación a {}: su solicitud {} fue {}", solicitud.getEstudiante().getEmail(),
                solicitud.getId(), solicitud.getEstado());
    }
}
