package ar.edu.utn.tup.solicitudes.aplicacion;

import java.time.Clock;
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
import ar.edu.utn.tup.solicitudes.dominio.excepciones.RecursoNoEncontradoException;
import ar.edu.utn.tup.solicitudes.persistencia.EstudianteRepository;
import ar.edu.utn.tup.solicitudes.persistencia.ResponsableRepository;
import ar.edu.utn.tup.solicitudes.persistencia.SolicitudRepository;

@Service
@Transactional
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final EstudianteRepository estudianteRepository;
    private final ResponsableRepository responsableRepository;
    private final Notificador notificador;
    private final Clock clock;

    public SolicitudService(SolicitudRepository solicitudRepository, EstudianteRepository estudianteRepository,
            ResponsableRepository responsableRepository, Notificador notificador, Clock clock) {
        this.solicitudRepository = solicitudRepository;
        this.estudianteRepository = estudianteRepository;
        this.responsableRepository = responsableRepository;
        this.notificador = notificador;
        this.clock = clock;
    }

    public SolicitudDetalle registrar(PedidoRegistro pedido) {
        NuevaSolicitud nueva = NuevaSolicitud.desde(pedido);
        Estudiante estudiante = buscarEstudiante(nueva.legajo());
        Solicitud solicitud = armarSolicitud(nueva, estudiante, ahoraTruncado());
        return aDetalle(solicitudRepository.save(solicitud));
    }

    public SolicitudDetalle obtener(Long solicitudId) {
        return aDetalle(buscarSolicitud(solicitudId));
    }

    public List<SolicitudDetalle> buscar(FiltroSolicitudes filtro) {
        List<Solicitud> coincidentes = new ArrayList<>();
        for (Solicitud solicitud : solicitudRepository.findAll()) {
            if (filtro.coincideCon(solicitud)) {
                coincidentes.add(solicitud);
            }
        }
        coincidentes.sort(Comparator.comparing(Solicitud::getId));
        return aDetalles(coincidentes);
    }

    public SolicitudDetalle cambiarEstado(Long solicitudId, String textoEstado) {
        Solicitud solicitud = buscarSolicitud(solicitudId);
        EstadoSolicitud nuevoEstado = EstadoSolicitud.parsear(textoEstado);
        solicitud.cambiarEstado(nuevoEstado, ahoraTruncado());
        Solicitud guardada = solicitudRepository.save(solicitud);
        if (guardada.getEstado().esResolucion()) {
            notificador.notificarCierre(guardada);
        }
        return aDetalle(guardada);
    }

    public SolicitudDetalle asignarResponsable(Long solicitudId, Long responsableId) {
        Solicitud solicitud = buscarSolicitud(solicitudId);
        solicitud.verificarQueAdmiteResponsable();
        Responsable responsable = buscarResponsable(responsableId);
        solicitud.asignarResponsable(responsable, ahoraTruncado());
        return aDetalle(solicitudRepository.save(solicitud));
    }

    private Solicitud buscarSolicitud(Long solicitudId) {
        return solicitudRepository.findById(solicitudId).orElseThrow(
                () -> new RecursoNoEncontradoException("Solicitud no encontrada: " + solicitudId));
    }

    private Estudiante buscarEstudiante(String legajo) {
        return estudianteRepository.findByLegajo(legajo).orElseThrow(
                () -> new RecursoNoEncontradoException("Estudiante no encontrado: " + legajo));
    }

    private Responsable buscarResponsable(Long responsableId) {
        Responsable responsable = responsableId == null ? null
                : responsableRepository.findById(responsableId).orElse(null);
        if (responsable == null) {
            throw new RecursoNoEncontradoException("Responsable no encontrado: " + responsableId);
        }
        return responsable;
    }

    private SolicitudDetalle aDetalle(Solicitud solicitud) {
        return new SolicitudDetalle(solicitud, solicitud.estaVencida(LocalDateTime.now(clock)));
    }

    private List<SolicitudDetalle> aDetalles(List<Solicitud> solicitudes) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return solicitudes.stream()
                .map(solicitud -> new SolicitudDetalle(solicitud, solicitud.estaVencida(ahora))).toList();
    }

    private LocalDateTime ahoraTruncado() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    private Solicitud armarSolicitud(NuevaSolicitud nueva, Estudiante estudiante, LocalDateTime creacion) {
        Solicitud solicitud = new Solicitud();
        solicitud.setTipo(nueva.tipo());
        solicitud.setDescripcion(nueva.descripcion());
        solicitud.setPrioridad(nueva.prioridadEfectiva());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaCreacion(creacion);
        solicitud.setFechaVencimiento(creacion.plusDays(nueva.plazoDias()));
        solicitud.setEstudiante(estudiante);
        return solicitud;
    }
}
