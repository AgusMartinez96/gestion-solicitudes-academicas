package ar.edu.utn.tup.solicitudes.util;

import java.time.LocalDateTime;

public class Utils {

    public static int calcularDias(String tipo) {
        switch (tipo) {
            case "CERTIFICADO_ALUMNO_REGULAR":
                return 2;
            case "CAMBIO_COMISION":
                return 3;
            case "REVISION_EXAMEN":
                return 5;
            case "OTRO":
                return 7;
            case "EQUIVALENCIA":
                return 15;
            default:
                return 7;
        }
    }

    public static LocalDateTime calcularVencimiento(LocalDateTime creacion, int dias) {
        return creacion.plusDays(dias);
    }

    public static void validar(String descripcion, String tipo, String prioridad, String legajo) {
        if (descripcion == null || descripcion.length() < 10 || descripcion.length() > 500) {
            throw new RuntimeException("400|La descripción debe tener entre 10 y 500 caracteres");
        }
        if (tipo == null || !(tipo.equals("CERTIFICADO_ALUMNO_REGULAR") || tipo.equals("REVISION_EXAMEN")
                || tipo.equals("EQUIVALENCIA") || tipo.equals("CAMBIO_COMISION") || tipo.equals("OTRO"))) {
            throw new RuntimeException("400|Tipo de solicitud inválido: " + tipo);
        }
        if (prioridad == null || !(prioridad.equals("BAJA") || prioridad.equals("MEDIA") || prioridad.equals("ALTA"))) {
            throw new RuntimeException("400|Prioridad inválida: " + prioridad);
        }
        if (legajo == null) {
            throw new RuntimeException("404|Estudiante no encontrado: " + legajo);
        }
    }
}
