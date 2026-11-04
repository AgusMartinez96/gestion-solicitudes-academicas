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
import ar.edu.utn.tup.solicitudes.model.Responsable;
import ar.edu.utn.tup.solicitudes.model.Solicitud;
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
            String tipo = (String) body.get("tipo");
            String descripcion = (String) body.get("descripcion");
            String prioridad = (String) body.get("prioridad");
            System.out.println("DEBUG crear solicitud legajo=" + legajo + " tipo=" + tipo);
            validarDescripcion(descripcion);
            validarTipo(tipo);
            prioridad = resolverPrioridad(tipo, prioridad);
            Utils.validar(descripcion, tipo, prioridad, legajo);
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
            validarFiltros(estado, tipo, prioridad);
            List<Solicitud> lista2 = filtrar(estado, tipo, prioridad, legajo);
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
            String nuevo = (String) body.get("estado");
            validarEstado(nuevo);
            if (!puedeCambiar(s, nuevo)) {
                throw new RuntimeException("409|Transición inválida: " + s.getEstado() + " -> " + nuevo);
            }
            if ((nuevo.equals("RESUELTA") || nuevo.equals("RECHAZADA")) && s.getResponsable() == null) {
                throw new RuntimeException(
                        "409|La solicitud requiere un responsable asignado para pasar a " + nuevo);
            }
            s.setEstado(nuevo);
            s.setFechaActualizacion(ahoraTruncado());
            s = solicitudRepository.save(s);
            if (s.getEstado().equals("RESUELTA") || s.getEstado().equals("RECHAZADA")) {
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
            if (s.getEstado().equals("PENDIENTE")) {
                s.setEstado("EN_ANALISIS");
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
        porEstado.put("PENDIENTE", 0);
        porEstado.put("EN_ANALISIS", 0);
        porEstado.put("RESUELTA", 0);
        porEstado.put("RECHAZADA", 0);
        porEstado.put("CANCELADA", 0);
        for (Solicitud s : todas) {
            porEstado.put(s.getEstado(), porEstado.get(s.getEstado()) + 1);
        }
        return porEstado;
    }

    private Map<String, Integer> contarPorTipo(List<Solicitud> todas) {
        Map<String, Integer> porTipo = new LinkedHashMap<>();
        porTipo.put("CERTIFICADO_ALUMNO_REGULAR", 0);
        porTipo.put("REVISION_EXAMEN", 0);
        porTipo.put("EQUIVALENCIA", 0);
        porTipo.put("CAMBIO_COMISION", 0);
        porTipo.put("OTRO", 0);
        for (Solicitud s : todas) {
            porTipo.put(s.getTipo(), porTipo.get(s.getTipo()) + 1);
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

    private void validarTipo(String tipo) {
        if (tipo == null || !(tipo.equals("CERTIFICADO_ALUMNO_REGULAR") || tipo.equals("REVISION_EXAMEN")
                || tipo.equals("EQUIVALENCIA") || tipo.equals("CAMBIO_COMISION") || tipo.equals("OTRO"))) {
            throw new RuntimeException("400|Tipo de solicitud inválido: " + tipo);
        }
    }

    private void validarEstado(String estado) {
        if (estado == null || !(estado.equals("PENDIENTE") || estado.equals("EN_ANALISIS")
                || estado.equals("RESUELTA") || estado.equals("RECHAZADA") || estado.equals("CANCELADA"))) {
            throw new RuntimeException("400|Estado inválido: " + estado);
        }
    }

    private void validarPrioridad(String prioridad) {
        if (prioridad == null || !(prioridad.equals("BAJA") || prioridad.equals("MEDIA") || prioridad.equals("ALTA"))) {
            throw new RuntimeException("400|Prioridad inválida: " + prioridad);
        }
    }

    private void validarFiltros(String estado, String tipo, String prioridad) {
        if (estado != null) {
            validarEstado(estado);
        }
        if (tipo != null) {
            validarTipo(tipo);
        }
        if (prioridad != null) {
            validarPrioridad(prioridad);
        }
    }

    private String resolverPrioridad(String tipo, String prioridad) {
        if (prioridad != null) {
            return prioridad;
        }
        if (tipo.equals("REVISION_EXAMEN")) {
            return "ALTA";
        }
        if (tipo.equals("CAMBIO_COMISION") || tipo.equals("CERTIFICADO_ALUMNO_REGULAR")) {
            return "MEDIA";
        }
        return "BAJA";
    }

    private int calcularPlazoDias(String tipo, String prioridad) {
        int dias = Utils.calcularDias(tipo);
        if (prioridad.equals("ALTA")) {
            dias = dias / 2;
            if (dias < 1) {
                dias = 1;
            }
        }
        return dias;
    }

    private List<Solicitud> filtrar(String estado, String tipo, String prioridad, String legajo) {
        List<Solicitud> lista = solicitudRepository.findAll();
        List<Solicitud> lista2 = new ArrayList<>();
        for (Solicitud s : lista) {
            if (estado == null || s.getEstado().equals(estado)) {
                if (tipo == null || s.getTipo().equals(tipo)) {
                    if (prioridad == null || s.getPrioridad().equals(prioridad)) {
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
        return s.getEstado().equals("RESUELTA") || s.getEstado().equals("RECHAZADA")
                || s.getEstado().equals("CANCELADA");
    }

    private Map<String, Object> aMap(Solicitud s) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", s.getId());
        res.put("tipo", s.getTipo());
        res.put("descripcion", s.getDescripcion());
        res.put("prioridad", s.getPrioridad());
        res.put("estado", s.getEstado());
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

    private boolean puedeCambiar(Solicitud s, String nuevo) {
        String actual = s.getEstado();
        if (actual.equals("PENDIENTE")) {
            if (nuevo.equals("EN_ANALISIS") || nuevo.equals("CANCELADA")) {
                return true;
            }
        } else if (actual.equals("EN_ANALISIS")) {
            if (nuevo.equals("RESUELTA") || nuevo.equals("RECHAZADA")) {
                return true;
            }
        }
        return false;
    }

    private Solicitud armarSolicitud(String legajo, String tipo, String descripcion, String prioridad,
            LocalDateTime creacion, int dias, Estudiante est) {
        Solicitud s = new Solicitud();
        s.setTipo(tipo);
        s.setDescripcion(descripcion);
        s.setPrioridad(prioridad);
        s.setEstado("PENDIENTE");
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
