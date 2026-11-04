package ar.edu.utn.tup.solicitudes.aplicacion;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.tup.solicitudes.dominio.EstadoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.Estudiante;
import ar.edu.utn.tup.solicitudes.dominio.Responsable;
import ar.edu.utn.tup.solicitudes.dominio.Solicitud;
import ar.edu.utn.tup.solicitudes.persistencia.EstudianteRepository;
import ar.edu.utn.tup.solicitudes.persistencia.ResponsableRepository;
import ar.edu.utn.tup.solicitudes.persistencia.SolicitudRepository;
import ar.edu.utn.tup.solicitudes.util.NotificadorConsola;
import ar.edu.utn.tup.solicitudes.util.Utils;

@Service
@Transactional
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final EstudianteRepository estudianteRepository;
    private final ResponsableRepository responsableRepository;

    public SolicitudService(SolicitudRepository solicitudRepository, EstudianteRepository estudianteRepository,
            ResponsableRepository responsableRepository) {
        this.solicitudRepository = solicitudRepository;
        this.estudianteRepository = estudianteRepository;
        this.responsableRepository = responsableRepository;
    }

    public Solicitud registrar(NuevaSolicitud nueva) {
        Estudiante est = buscarEstudiante(nueva.legajo());
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
        Solicitud s = armarSolicitud(nueva, est, ahora);
        return solicitudRepository.save(s);
    }

    public Solicitud obtener(Long id) {
        return buscarSolicitud(id);
    }

    public List<Solicitud> buscar(FiltroSolicitudes filtro) {
        List<Solicitud> lista2 = new ArrayList<>();
        for (Solicitud s : solicitudRepository.findAll()) {
            if (filtro.coincideCon(s)) {
                lista2.add(s);
            }
        }
        lista2.sort(Comparator.comparing(Solicitud::getId));
        return lista2;
    }

    public Solicitud cambiarEstado(Long id, String estado) {
        Solicitud s = buscarSolicitud(id);
        EstadoSolicitud nuevo = parsearEstado(estado);
        if (!s.getEstado().puedeTransicionarA(nuevo)) {
            throw new RuntimeException("409|Transición inválida: " + s.getEstado() + " -> " + nuevo);
        }
        if (nuevo.requiereResponsable() && !s.tieneResponsable()) {
            throw new RuntimeException("409|La solicitud requiere un responsable asignado para pasar a " + nuevo);
        }
        s.cambiarEstado(nuevo, ahoraTruncado());
        s = solicitudRepository.save(s);
        if (s.getEstado().esResolucion()) {
            new NotificadorConsola().notificar(s.getEstudiante().getEmail(),
                    "Su solicitud " + s.getId() + " fue " + s.getEstado());
        }
        return s;
    }

    public Solicitud asignarResponsable(Long id, Long responsableId) {
        Solicitud s = buscarSolicitud(id);
        if (s.estaCerrada()) {
            throw new RuntimeException("409|La solicitud está cerrada: " + id);
        }
        Responsable resp = buscarResponsableActivo(responsableId);
        s.asignarResponsable(resp, ahoraTruncado());
        return solicitudRepository.save(s);
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

    private EstadoSolicitud parsearEstado(String texto) {
        return EstadoSolicitud.desde(texto)
                .orElseThrow(() -> new RuntimeException("400|Estado inválido: " + texto));
    }

    private LocalDateTime ahoraTruncado() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private Solicitud armarSolicitud(NuevaSolicitud nueva, Estudiante est, LocalDateTime creacion) {
        Solicitud s = new Solicitud();
        s.setTipo(nueva.tipo());
        s.setDescripcion(nueva.descripcion());
        s.setPrioridad(nueva.prioridadEfectiva());
        s.setEstado(EstadoSolicitud.PENDIENTE);
        s.setFechaCreacion(creacion);
        s.setFechaVencimiento(Utils.calcularVencimiento(creacion, nueva.plazoDias()));
        s.setEstudiante(est);
        System.out.println("DEBUG solicitud armada para " + nueva.legajo() + " vence en " + nueva.plazoDias() + " dias");
        return s;
    }
}
