package ar.edu.utn.tup.solicitudes.dominio;

import ar.edu.utn.tup.solicitudes.dominio.excepciones.DatoInvalidoException;

public enum TipoSolicitud {

    CERTIFICADO_ALUMNO_REGULAR(2, Prioridad.MEDIA),
    REVISION_EXAMEN(5, Prioridad.ALTA),
    EQUIVALENCIA(15, Prioridad.BAJA),
    CAMBIO_COMISION(3, Prioridad.MEDIA),
    OTRO(7, Prioridad.BAJA);

    private final int plazoDias;
    private final Prioridad prioridadPorDefecto;

    TipoSolicitud(int plazoDias, Prioridad prioridadPorDefecto) {
        this.plazoDias = plazoDias;
        this.prioridadPorDefecto = prioridadPorDefecto;
    }

    public int plazoDias() {
        return plazoDias;
    }

    public Prioridad prioridadPorDefecto() {
        return prioridadPorDefecto;
    }

    public static TipoSolicitud parsear(String valor) {
        for (TipoSolicitud tipo : values()) {
            if (tipo.name().equals(valor)) {
                return tipo;
            }
        }
        throw new DatoInvalidoException("Tipo de solicitud inválido: " + valor);
    }
}
