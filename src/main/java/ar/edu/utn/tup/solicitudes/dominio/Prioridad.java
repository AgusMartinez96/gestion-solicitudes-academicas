package ar.edu.utn.tup.solicitudes.dominio;

import java.util.Optional;

public enum Prioridad {

    BAJA,
    MEDIA,
    ALTA;

    public int ajustarPlazo(int dias) {
        if (this == ALTA) {
            return Math.max(1, dias / 2);
        }
        return dias;
    }

    public static Optional<Prioridad> desde(String valor) {
        for (Prioridad prioridad : values()) {
            if (prioridad.name().equals(valor)) {
                return Optional.of(prioridad);
            }
        }
        return Optional.empty();
    }
}
