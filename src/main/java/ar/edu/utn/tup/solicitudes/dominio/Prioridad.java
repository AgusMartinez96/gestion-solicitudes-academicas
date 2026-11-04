package ar.edu.utn.tup.solicitudes.dominio;

import java.util.Optional;

public enum Prioridad {

    BAJA,
    MEDIA,
    ALTA;

    private static final int DIVISOR_PLAZO_ALTA = 2;
    private static final int PLAZO_MINIMO_DIAS = 1;

    public int ajustarPlazo(int dias) {
        if (this == ALTA) {
            return Math.max(PLAZO_MINIMO_DIAS, dias / DIVISOR_PLAZO_ALTA);
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
