package ar.edu.utn.tup.solicitudes.model;

import java.util.Optional;

public enum EstadoSolicitud {

    PENDIENTE,
    EN_ANALISIS,
    RESUELTA,
    RECHAZADA,
    CANCELADA;

    public static Optional<EstadoSolicitud> desde(String valor) {
        for (EstadoSolicitud estado : values()) {
            if (estado.name().equals(valor)) {
                return Optional.of(estado);
            }
        }
        return Optional.empty();
    }
}
