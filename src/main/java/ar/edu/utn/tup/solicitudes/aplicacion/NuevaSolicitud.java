package ar.edu.utn.tup.solicitudes.aplicacion;

import ar.edu.utn.tup.solicitudes.dominio.Prioridad;
import ar.edu.utn.tup.solicitudes.dominio.TipoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.excepciones.DatoInvalidoException;

public record NuevaSolicitud(String legajo, TipoSolicitud tipo, String descripcion, Prioridad prioridad) {

    private static final int LONGITUD_MINIMA_DESCRIPCION = 10;
    private static final int LONGITUD_MAXIMA_DESCRIPCION = 500;

    public static NuevaSolicitud desde(PedidoRegistro pedido) {
        validarDescripcion(pedido.descripcion());
        TipoSolicitud tipo = TipoSolicitud.parsear(pedido.tipo());
        Prioridad prioridad = pedido.prioridad() == null ? null : Prioridad.parsear(pedido.prioridad());
        return new NuevaSolicitud(pedido.legajo(), tipo, pedido.descripcion(), prioridad);
    }

    private static void validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.length() < LONGITUD_MINIMA_DESCRIPCION
                || descripcion.length() > LONGITUD_MAXIMA_DESCRIPCION) {
            throw new DatoInvalidoException("La descripción debe tener entre " + LONGITUD_MINIMA_DESCRIPCION
                    + " y " + LONGITUD_MAXIMA_DESCRIPCION + " caracteres");
        }
    }

    public Prioridad prioridadEfectiva() {
        if (prioridad != null) {
            return prioridad;
        }
        return tipo.prioridadPorDefecto();
    }

    public int plazoDias() {
        return prioridadEfectiva().ajustarPlazo(tipo.plazoDias());
    }
}
