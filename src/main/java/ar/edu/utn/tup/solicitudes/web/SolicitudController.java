package ar.edu.utn.tup.solicitudes.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.tup.solicitudes.aplicacion.FiltroSolicitudes;
import ar.edu.utn.tup.solicitudes.aplicacion.NuevaSolicitud;
import ar.edu.utn.tup.solicitudes.aplicacion.SolicitudService;
import ar.edu.utn.tup.solicitudes.web.dto.AsignarResponsableRequest;
import ar.edu.utn.tup.solicitudes.web.dto.CambiarEstadoRequest;
import ar.edu.utn.tup.solicitudes.web.dto.RegistrarSolicitudRequest;
import ar.edu.utn.tup.solicitudes.web.dto.SolicitudResponse;

@RestController
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final SolicitudMapper mapper;

    public SolicitudController(SolicitudService solicitudService, SolicitudMapper mapper) {
        this.solicitudService = solicitudService;
        this.mapper = mapper;
    }

    @PostMapping("/api/solicitudes")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudResponse crear(@RequestBody RegistrarSolicitudRequest request) {
        System.out.println("DEBUG crear solicitud legajo=" + request.legajo() + " tipo=" + request.tipo());
        NuevaSolicitud nueva = mapper.aNuevaSolicitud(request);
        return mapper.aResponse(solicitudService.registrar(nueva));
    }

    @GetMapping("/api/solicitudes/{id}")
    public SolicitudResponse obtener(@PathVariable("id") Long id) {
        return mapper.aResponse(solicitudService.obtener(id));
    }

    @GetMapping("/api/solicitudes")
    public List<SolicitudResponse> listar(@RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "prioridad", required = false) String prioridad,
            @RequestParam(name = "legajo", required = false) String legajo) {
        FiltroSolicitudes filtro = new FiltroSolicitudes(
                estado == null ? null : mapper.aEstado(estado),
                tipo == null ? null : mapper.aTipo(tipo),
                prioridad == null ? null : mapper.aPrioridad(prioridad),
                legajo);
        return mapper.aResponse(solicitudService.buscar(filtro));
    }

    @PatchMapping("/api/solicitudes/{id}/estado")
    public SolicitudResponse cambiarEstado(@PathVariable("id") Long id,
            @RequestBody CambiarEstadoRequest request) {
        return mapper.aResponse(solicitudService.cambiarEstado(id, request.estado()));
    }

    @PatchMapping("/api/solicitudes/{id}/responsable")
    public SolicitudResponse asignarResponsable(@PathVariable("id") Long id,
            @RequestBody AsignarResponsableRequest request) {
        return mapper.aResponse(solicitudService.asignarResponsable(id, request.responsableId()));
    }
}
