package ar.edu.utn.tup.solicitudes.util;

import java.time.LocalDateTime;

public class Utils {

    public static LocalDateTime calcularVencimiento(LocalDateTime creacion, int dias) {
        return creacion.plusDays(dias);
    }
}
