package ar.edu.utn.tup.solicitudes.web.dto;

public record RegistrarSolicitudRequest(String legajo, String tipo, String descripcion, String prioridad) {
}
