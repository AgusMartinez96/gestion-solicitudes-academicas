package ar.edu.utn.tup.solicitudes.model;

import java.util.Optional;

public enum EstadoSolicitud {

    PENDIENTE,
    EN_ANALISIS,
    RESUELTA,
    RECHAZADA,
    CANCELADA;

    public boolean puedeTransicionarA(EstadoSolicitud destino) {
        return switch (this) {
            case PENDIENTE -> destino == EN_ANALISIS || destino == CANCELADA;
            case EN_ANALISIS -> destino == RESUELTA || destino == RECHAZADA;
            case RESUELTA, RECHAZADA, CANCELADA -> false;
        };
    }

    public boolean esCerrado() {
        return this == RESUELTA || this == RECHAZADA || this == CANCELADA;
    }

    public boolean esResolucion() {
        return this == RESUELTA || this == RECHAZADA;
    }

    public boolean requiereResponsable() {
        return esResolucion();
    }

    public static Optional<EstadoSolicitud> desde(String valor) {
        for (EstadoSolicitud estado : values()) {
            if (estado.name().equals(valor)) {
                return Optional.of(estado);
            }
        }
        return Optional.empty();
    }
}
