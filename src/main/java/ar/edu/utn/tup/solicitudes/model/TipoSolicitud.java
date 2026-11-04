package ar.edu.utn.tup.solicitudes.model;

import java.util.Optional;

public enum TipoSolicitud {

    CERTIFICADO_ALUMNO_REGULAR,
    REVISION_EXAMEN,
    EQUIVALENCIA,
    CAMBIO_COMISION,
    OTRO;

    public static Optional<TipoSolicitud> desde(String valor) {
        for (TipoSolicitud tipo : values()) {
            if (tipo.name().equals(valor)) {
                return Optional.of(tipo);
            }
        }
        return Optional.empty();
    }
}
