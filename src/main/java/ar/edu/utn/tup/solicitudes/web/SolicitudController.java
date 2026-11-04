package ar.edu.utn.tup.solicitudes.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.tup.solicitudes.aplicacion.FiltroSolicitudes;
import ar.edu.utn.tup.solicitudes.aplicacion.SolicitudService;
import ar.edu.utn.tup.solicitudes.dominio.EstadoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.Prioridad;
import ar.edu.utn.tup.solicitudes.dominio.TipoSolicitud;
import ar.edu.utn.tup.solicitudes.web.dto.AsignarResponsableRequest;
import ar.edu.utn.tup.solicitudes.web.dto.CambiarEstadoRequest;
import ar.edu.utn.tup.solicitudes.web.dto.ErrorResponse;
import ar.edu.utn.tup.solicitudes.web.dto.RegistrarSolicitudRequest;
import ar.edu.utn.tup.solicitudes.web.dto.SolicitudResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final SolicitudMapper mapper;

    public SolicitudController(SolicitudService solicitudService, SolicitudMapper mapper) {
        this.solicitudService = solicitudService;
        this.mapper = mapper;
    }

    @ApiResponse(responseCode = "400", description = "Dato inválido (descripción, tipo o prioridad)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/api/solicitudes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudResponse crear(@RequestBody RegistrarSolicitudRequest request) {
        return mapper.aResponse(solicitudService.registrar(mapper.aPedido(request)));
    }

    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Identificador no numérico",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping(value = "/api/solicitudes/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public SolicitudResponse obtener(@PathVariable("id") Long solicitudId) {
        return mapper.aResponse(solicitudService.obtener(solicitudId));
    }

    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Estado, tipo o prioridad inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping(value = "/api/solicitudes", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SolicitudResponse> listar(@RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "prioridad", required = false) String prioridad,
            @RequestParam(name = "legajo", required = false) String legajo) {
        FiltroSolicitudes filtro = new FiltroSolicitudes(
                estado == null ? null : EstadoSolicitud.parsear(estado),
                tipo == null ? null : TipoSolicitud.parsear(tipo),
                prioridad == null ? null : Prioridad.parsear(prioridad),
                legajo);
        return mapper.aResponse(solicitudService.buscar(filtro));
    }

    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Estado inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Transición no permitida o falta responsable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping(value = "/api/solicitudes/{id}/estado", produces = MediaType.APPLICATION_JSON_VALUE)
    public SolicitudResponse cambiarEstado(@PathVariable("id") Long solicitudId,
            @RequestBody CambiarEstadoRequest request) {
        return mapper.aResponse(solicitudService.cambiarEstado(solicitudId, request.estado()));
    }

    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Solicitud o responsable no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Solicitud cerrada o responsable inactivo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping(value = "/api/solicitudes/{id}/responsable", produces = MediaType.APPLICATION_JSON_VALUE)
    public SolicitudResponse asignarResponsable(@PathVariable("id") Long solicitudId,
            @RequestBody AsignarResponsableRequest request) {
        return mapper.aResponse(solicitudService.asignarResponsable(solicitudId, request.responsableId()));
    }
}
