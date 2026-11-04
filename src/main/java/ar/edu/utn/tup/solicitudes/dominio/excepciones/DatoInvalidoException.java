package ar.edu.utn.tup.solicitudes.dominio.excepciones;

public class DatoInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
