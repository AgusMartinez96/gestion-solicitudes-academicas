package ar.edu.utn.tup.solicitudes.controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.tup.solicitudes.model.Estudiante;
import ar.edu.utn.tup.solicitudes.model.EstadoSolicitud;
import ar.edu.utn.tup.solicitudes.model.Prioridad;
import ar.edu.utn.tup.solicitudes.model.Responsable;
import ar.edu.utn.tup.solicitudes.model.Solicitud;
import ar.edu.utn.tup.solicitudes.model.TipoSolicitud;
import ar.edu.utn.tup.solicitudes.repository.EstudianteRepository;
import ar.edu.utn.tup.solicitudes.repository.ResponsableRepository;
import ar.edu.utn.tup.solicitudes.repository.SolicitudRepository;
import ar.edu.utn.tup.solicitudes.util.NotificadorConsola;
import ar.edu.utn.tup.solicitudes.util.Utils;

@RestController
@Transactional
public class SolicitudController {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private ResponsableRepository responsableRepository;

    @PostMapping("/api/solicitudes")
    public ResponseEntity<Object> crear(@RequestBody Map<String, Object> body) {
        try {
            String legajo = (String) body.get("legajo");
            String textoTipo = (String) body.get("tipo");
            String descripcion = (String) body.get("descripcion");
            String textoPrioridad = (String) body.get("prioridad");
            System.out.println("DEBUG crear solicitud legajo=" + legajo + " tipo=" + textoTipo);
            validarDescripcion(descripcion);
            TipoSolicitud tipo = parsearTipo(textoTipo);
            Prioridad prioridad = resolverPrioridad(tipo, textoPrioridad);
            Utils.validar(descripcion, tipo.name(), prioridad.name(), legajo);
            Estudiante est = buscarEstudiante(legajo);
            LocalDateTime ahora = ahoraTruncado();
            // int dias = 2;
            // if (tipo.equals("REVISION_EXAMEN")) {
            //     dias = 5;
            // } else if (tipo.equals("CAMBIO_COMISION")) {
            //     dias = 3;
            // }
            // if (prioridad.equals("ALTA")) {
            //     dias = dias - 1;
            // }
            int dias = calcularPlazoDias(tipo, prioridad);
            Solicitud s = armarSolicitud(legajo, tipo, descripcion, prioridad, ahora, dias, est);
            s = solicitudRepository.save(s);
            return ResponseEntity.status(HttpStatus.CREATED).body(aMap(s));
        } catch (Exception e) {
            return procesar(e);
        }
    }

    @GetMapping("/api/solicitudes/{id}")
    public ResponseEntity<Object> obtener(@PathVariable("id") Long id) {
        try {
            Solicitud s = buscarSolicitud(id);
            return ResponseEntity.ok(aMap(s));
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
            EstadoSolicitud estadoFiltro = estado == null ? null : parsearEstado(estado);
            TipoSolicitud tipoFiltro = tipo == null ? null : parsearTipo(tipo);
            Prioridad prioridadFiltro = prioridad == null ? null : parsearPrioridad(prioridad);
            List<Solicitud> lista2 = filtrar(estadoFiltro, tipoFiltro, prioridadFiltro, legajo);
            lista2.sort(Comparator.comparing(Solicitud::getId));
            List<Map<String, Object>> salida = new ArrayList<>();
            for (Solicitud s : lista2) {
                salida.add(aMap(s));
            }
            return ResponseEntity.ok(salida);
        } catch (Exception e) {
            return procesar(e);
        }
    }

    @PatchMapping("/api/solicitudes/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(@PathVariable("id") Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Solicitud s = buscarSolicitud(id);
            EstadoSolicitud nuevo = parsearEstado((String) body.get("estado"));
            if (!puedeCambiar(s, nuevo)) {
                throw new RuntimeException("409|Transición inválida: " + s.getEstado() + " -> " + nuevo);
            }
            if ((nuevo == EstadoSolicitud.RESUELTA || nuevo == EstadoSolicitud.RECHAZADA)
                    && s.getResponsable() == null) {
                throw new RuntimeException(
                        "409|La solicitud requiere un responsable asignado para pasar a " + nuevo);
            }
            s.setEstado(nuevo);
            s.setFechaActualizacion(ahoraTruncado());
            s = solicitudRepository.save(s);
            if (s.getEstado() == EstadoSolicitud.RESUELTA || s.getEstado() == EstadoSolicitud.RECHAZADA) {
                new NotificadorConsola().notificar(s.getEstudiante().getEmail(),
                        "Su solicitud " + s.getId() + " fue " + s.getEstado());
            }
            return ResponseEntity.ok(aMap(s));
        } catch (Exception e) {
            return procesar(e);
        }
    }

    @PatchMapping("/api/solicitudes/{id}/responsable")
    public ResponseEntity<Object> asignarResponsable(@PathVariable("id") Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Solicitud s = buscarSolicitud(id);
            if (estaCerrada(s)) {
                throw new RuntimeException("409|La solicitud está cerrada: " + id);
            }
            Object dato = body.get("responsableId");
            Long idResp = dato == null ? null : Long.valueOf(dato.toString());
            Responsable resp = buscarResponsableActivo(idResp);
            s.setResponsable(resp);
            if (s.getEstado() == EstadoSolicitud.PENDIENTE) {
                s.setEstado(EstadoSolicitud.EN_ANALISIS);
                s.setFechaActualizacion(ahoraTruncado());
            }
            s = solicitudRepository.save(s);
            return ResponseEntity.ok(aMap(s));
        } catch (Exception e) {
            return procesar(e);
        }
    }

    @GetMapping("/api/reportes/resumen")
    public ResponseEntity<Object> reporte() {
        try {
            List<Solicitud> todas = solicitudRepository.findAll();
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("total", todas.size());
            res.put("porEstado", contarPorEstado(todas));
            res.put("porTipo", contarPorTipo(todas));
            res.put("vencidas", contarVencidas(todas));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return procesar(e);
        }
    }

    private Map<String, Integer> contarPorEstado(List<Solicitud> todas) {
        Map<String, Integer> porEstado = new LinkedHashMap<>();
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            porEstado.put(estado.name(), 0);
        }
        for (Solicitud s : todas) {
            porEstado.put(s.getEstado().name(), porEstado.get(s.getEstado().name()) + 1);
        }
        return porEstado;
    }

    private Map<String, Integer> contarPorTipo(List<Solicitud> todas) {
        Map<String, Integer> porTipo = new LinkedHashMap<>();
        for (TipoSolicitud tipo : TipoSolicitud.values()) {
            porTipo.put(tipo.name(), 0);
        }
        for (Solicitud s : todas) {
            porTipo.put(s.getTipo().name(), porTipo.get(s.getTipo().name()) + 1);
        }
        return porTipo;
    }

    private int contarVencidas(List<Solicitud> todas) {
        int vencidas = 0;
        for (Solicitud s : todas) {
            if (!estaCerrada(s) && s.getFechaVencimiento().isBefore(LocalDateTime.now())) {
                vencidas++;
            }
        }
        return vencidas;
    }

    private Solicitud buscarSolicitud(Long id) {
        Solicitud s = solicitudRepository.findById(id).orElse(null);
        if (s == null) {
            throw new RuntimeException("404|Solicitud no encontrada: " + id);
        }
        return s;
    }

    private Estudiante buscarEstudiante(String legajo) {
        Estudiante est = estudianteRepository.findByLegajo(legajo).orElse(null);
        if (est == null) {
            throw new RuntimeException("404|Estudiante no encontrado: " + legajo);
        }
        return est;
    }

    private Responsable buscarResponsableActivo(Long idResp) {
        Responsable resp = idResp == null ? null : responsableRepository.findById(idResp).orElse(null);
        if (resp == null) {
            throw new RuntimeException("404|Responsable no encontrado: " + idResp);
        }
        if (!resp.isActivo()) {
            throw new RuntimeException("409|El responsable no está activo: " + idResp);
        }
        return resp;
    }

    private void validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.length() < 10 || descripcion.length() > 500) {
            throw new RuntimeException("400|La descripción debe tener entre 10 y 500 caracteres");
        }
    }

    private TipoSolicitud parsearTipo(String texto) {
        return TipoSolicitud.desde(texto)
                .orElseThrow(() -> new RuntimeException("400|Tipo de solicitud inválido: " + texto));
    }

    private EstadoSolicitud parsearEstado(String texto) {
        return EstadoSolicitud.desde(texto)
                .orElseThrow(() -> new RuntimeException("400|Estado inválido: " + texto));
    }

    private Prioridad parsearPrioridad(String texto) {
        return Prioridad.desde(texto)
                .orElseThrow(() -> new RuntimeException("400|Prioridad inválida: " + texto));
    }

    private Prioridad resolverPrioridad(TipoSolicitud tipo, String texto) {
        if (texto != null) {
            return parsearPrioridad(texto);
        }
        if (tipo == TipoSolicitud.REVISION_EXAMEN) {
            return Prioridad.ALTA;
        }
        if (tipo == TipoSolicitud.CAMBIO_COMISION || tipo == TipoSolicitud.CERTIFICADO_ALUMNO_REGULAR) {
            return Prioridad.MEDIA;
        }
        return Prioridad.BAJA;
    }

    private int calcularPlazoDias(TipoSolicitud tipo, Prioridad prioridad) {
        int dias = Utils.calcularDias(tipo.name());
        if (prioridad == Prioridad.ALTA) {
            dias = dias / 2;
            if (dias < 1) {
                dias = 1;
            }
        }
        return dias;
    }

    private List<Solicitud> filtrar(EstadoSolicitud estado, TipoSolicitud tipo, Prioridad prioridad,
            String legajo) {
        List<Solicitud> lista = solicitudRepository.findAll();
        List<Solicitud> lista2 = new ArrayList<>();
        for (Solicitud s : lista) {
            if (estado == null || s.getEstado() == estado) {
                if (tipo == null || s.getTipo() == tipo) {
                    if (prioridad == null || s.getPrioridad() == prioridad) {
                        if (legajo == null || s.getEstudiante().getLegajo().equals(legajo)) {
                            lista2.add(s);
                        }
                    }
                }
            }
        }
        return lista2;
    }

    private LocalDateTime ahoraTruncado() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private boolean estaCerrada(Solicitud s) {
        return s.getEstado() == EstadoSolicitud.RESUELTA || s.getEstado() == EstadoSolicitud.RECHAZADA
                || s.getEstado() == EstadoSolicitud.CANCELADA;
    }

    private Map<String, Object> aMap(Solicitud s) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", s.getId());
        res.put("tipo", s.getTipo().name());
        res.put("descripcion", s.getDescripcion());
        res.put("prioridad", s.getPrioridad().name());
        res.put("estado", s.getEstado().name());
        res.put("fechaCreacion", s.getFechaCreacion());
        res.put("fechaVencimiento", s.getFechaVencimiento());
        res.put("fechaActualizacion", s.getFechaActualizacion());
        res.put("vencida", !estaCerrada(s) && s.getFechaVencimiento().isBefore(LocalDateTime.now()));
        res.put("estudiante", aMapEstudiante(s.getEstudiante()));
        res.put("responsable", aMapResponsable(s.getResponsable()));
        return res;
    }

    private Map<String, Object> aMapEstudiante(Estudiante est) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("legajo", est.getLegajo());
        e.put("nombre", est.getNombre());
        return e;
    }

    private Map<String, Object> aMapResponsable(Responsable resp) {
        if (resp == null) {
            return null;
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", resp.getId());
        r.put("nombre", resp.getNombre());
        r.put("area", resp.getArea());
        return r;
    }

    private boolean puedeCambiar(Solicitud s, EstadoSolicitud nuevo) {
        EstadoSolicitud actual = s.getEstado();
        if (actual == EstadoSolicitud.PENDIENTE) {
            if (nuevo == EstadoSolicitud.EN_ANALISIS || nuevo == EstadoSolicitud.CANCELADA) {
                return true;
            }
        } else if (actual == EstadoSolicitud.EN_ANALISIS) {
            if (nuevo == EstadoSolicitud.RESUELTA || nuevo == EstadoSolicitud.RECHAZADA) {
                return true;
            }
        }
        return false;
    }

    private Solicitud armarSolicitud(String legajo, TipoSolicitud tipo, String descripcion, Prioridad prioridad,
            LocalDateTime creacion, int dias, Estudiante est) {
        Solicitud s = new Solicitud();
        s.setTipo(tipo);
        s.setDescripcion(descripcion);
        s.setPrioridad(prioridad);
        s.setEstado(EstadoSolicitud.PENDIENTE);
        s.setFechaCreacion(creacion);
        s.setFechaVencimiento(Utils.calcularVencimiento(creacion, dias));
        s.setEstudiante(est);
        System.out.println("DEBUG solicitud armada para " + legajo + " vence en " + dias + " dias");
        return s;
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
