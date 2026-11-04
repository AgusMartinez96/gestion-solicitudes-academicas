package ar.edu.utn.tup.solicitudes.model;

import java.util.Optional;

public enum Prioridad {

    BAJA,
    MEDIA,
    ALTA;

    public static Optional<Prioridad> desde(String valor) {
        for (Prioridad prioridad : values()) {
            if (prioridad.name().equals(valor)) {
                return Optional.of(prioridad);
            }
        }
        return Optional.empty();
    }
}
