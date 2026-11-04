package ar.edu.utn.tup.solicitudes.web.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(int status, String error, String mensaje,
        @Schema(type = "string", format = "date-time", example = "2026-11-04T10:29:24") LocalDateTime timestamp) {
}
