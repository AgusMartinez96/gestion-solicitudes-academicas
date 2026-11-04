package ar.edu.utn.tup.solicitudes.dominio;

import ar.edu.utn.tup.solicitudes.dominio.excepciones.DatoInvalidoException;

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

    public static EstadoSolicitud parsear(String valor) {
        for (EstadoSolicitud estado : values()) {
            if (estado.name().equals(valor)) {
                return estado;
            }
        }
        throw new DatoInvalidoException("Estado inválido: " + valor);
    }
}
