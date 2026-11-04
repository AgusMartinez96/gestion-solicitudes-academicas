package ar.edu.utn.tup.solicitudes.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.tup.solicitudes.aplicacion.FiltroSolicitudes;
import ar.edu.utn.tup.solicitudes.aplicacion.NuevaSolicitud;
import ar.edu.utn.tup.solicitudes.aplicacion.SolicitudService;
import ar.edu.utn.tup.solicitudes.dominio.Solicitud;
import ar.edu.utn.tup.solicitudes.web.dto.AsignarResponsableRequest;
import ar.edu.utn.tup.solicitudes.web.dto.CambiarEstadoRequest;
import ar.edu.utn.tup.solicitudes.web.dto.RegistrarSolicitudRequest;

@RestController
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final SolicitudMapper mapper;

    public SolicitudController(SolicitudService solicitudService, SolicitudMapper mapper) {
        this.solicitudService = solicitudService;
        this.mapper = mapper;
    }

    @PostMapping("/api/solicitudes")
    public ResponseEntity<Object> crear(@RequestBody RegistrarSolicitudRequest request) {
        try {
            System.out.println("DEBUG crear solicitud legajo=" + request.legajo() + " tipo=" + request.tipo());
            NuevaSolicitud nueva = mapper.aNuevaSolicitud(request);
            Solicitud s = solicitudService.registrar(nueva);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aMap(s));
        } catch (Exception e) {
            return procesar(e);
        }
    }

    @GetMapping("/api/solicitudes/{id}")
    public ResponseEntity<Object> obtener(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(mapper.aMap(solicitudService.obtener(id)));
        } catch (Exception e) {
            return procesar(e);
        }
    }

    @GetMapping("/api/solicitudes")
    public ResponseEntity<Object> listar(@RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "prioridad", required = false) String prioridad,
            @RequestParam(name = "legajo", required = false) String legajo) {
        try {
            FiltroSolicitudes filtro = new FiltroSolicitudes(
                    estado == null ? null : mapper.aEstado(estado),
                    tipo == null ? null : mapper.aTipo(tipo),
                    prioridad == null ? null : mapper.aPrioridad(prioridad),
                    legajo);
            List<Solicitud> encontradas = solicitudService.buscar(filtro);
            return ResponseEntity.ok(mapper.aMap(encontradas));
        } catch (Exception e) {
            return procesar(e);
        }
    }

    @PatchMapping("/api/solicitudes/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(@PathVariable("id") Long id,
            @RequestBody CambiarEstadoRequest request) {
        try {
            return ResponseEntity.ok(mapper.aMap(solicitudService.cambiarEstado(id, request.estado())));
        } catch (Exception e) {
            return procesar(e);
        }
    }

    @PatchMapping("/api/solicitudes/{id}/responsable")
    public ResponseEntity<Object> asignarResponsable(@PathVariable("id") Long id,
            @RequestBody AsignarResponsableRequest request) {
        try {
            return ResponseEntity.ok(mapper.aMap(solicitudService.asignarResponsable(id, request.responsableId())));
        } catch (Exception e) {
            return procesar(e);
        }
    }

    private ResponseEntity<Object> procesar(Exception e) {
        String msg = e.getMessage();
        int codigo = 500;
        if (msg != null && msg.startsWith("404|")) {
            codigo = 404;
            msg = msg.substring(4);
        } else if (msg != null && msg.startsWith("409|")) {
            codigo = 409;
            msg = msg.substring(4);
        } else if (msg != null && msg.startsWith("400|")) {
            codigo = 400;
            msg = msg.substring(4);
        }
        return ResponseEntity.status(codigo).contentType(MediaType.valueOf("text/plain;charset=UTF-8"))
                .body("Error: " + msg);
    }
}
