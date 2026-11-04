package ar.edu.utn.tup.solicitudes.web.dto;

import java.time.LocalDateTime;

public record ErrorResponse(int status, String error, String mensaje, LocalDateTime timestamp) {
}
