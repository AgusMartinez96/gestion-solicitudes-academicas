package ar.edu.utn.tup.solicitudes.aplicacion;

import ar.edu.utn.tup.solicitudes.dominio.NuevaSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.Prioridad;
import ar.edu.utn.tup.solicitudes.dominio.TipoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.excepciones.DatoInvalidoException;

public record PedidoRegistro(String legajo, String tipo, String descripcion, String prioridad) {

    private static final int LONGITUD_MINIMA_DESCRIPCION = 10;
    private static final int LONGITUD_MAXIMA_DESCRIPCION = 500;

    public NuevaSolicitud validar() {
        validarDescripcion();
        TipoSolicitud tipoValido = TipoSolicitud.parsear(tipo);
        Prioridad prioridadValida = prioridad == null ? null : Prioridad.parsear(prioridad);
        return new NuevaSolicitud(legajo, tipoValido, descripcion, prioridadValida);
    }

    private void validarDescripcion() {
        if (descripcion == null || descripcion.length() < LONGITUD_MINIMA_DESCRIPCION
                || descripcion.length() > LONGITUD_MAXIMA_DESCRIPCION) {
            throw new DatoInvalidoException("La descripción debe tener entre " + LONGITUD_MINIMA_DESCRIPCION
                    + " y " + LONGITUD_MAXIMA_DESCRIPCION + " caracteres");
        }
    }
}
