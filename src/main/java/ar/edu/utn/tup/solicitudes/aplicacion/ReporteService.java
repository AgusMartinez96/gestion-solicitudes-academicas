package ar.edu.utn.tup.solicitudes.aplicacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.tup.solicitudes.dominio.EstadoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.Solicitud;
import ar.edu.utn.tup.solicitudes.dominio.TipoSolicitud;
import ar.edu.utn.tup.solicitudes.persistencia.SolicitudRepository;

@Service
@Transactional
public class ReporteService {

    private final SolicitudRepository solicitudRepository;
    private final Clock clock;

    public ReporteService(SolicitudRepository solicitudRepository, Clock clock) {
        this.solicitudRepository = solicitudRepository;
        this.clock = clock;
    }

    public ReporteResumen resumen() {
        List<Solicitud> todas = solicitudRepository.findAll();
        return new ReporteResumen(todas.size(), contarPorEstado(todas), contarPorTipo(todas),
                contarVencidas(todas));
    }

    private Map<EstadoSolicitud, Integer> contarPorEstado(List<Solicitud> todas) {
        Map<EstadoSolicitud, Integer> porEstado = new EnumMap<>(EstadoSolicitud.class);
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            porEstado.put(estado, 0);
        }
        for (Solicitud solicitud : todas) {
            porEstado.merge(solicitud.getEstado(), 1, Integer::sum);
        }
        return porEstado;
    }

    private Map<TipoSolicitud, Integer> contarPorTipo(List<Solicitud> todas) {
        Map<TipoSolicitud, Integer> porTipo = new EnumMap<>(TipoSolicitud.class);
        for (TipoSolicitud tipo : TipoSolicitud.values()) {
            porTipo.put(tipo, 0);
        }
        for (Solicitud solicitud : todas) {
            porTipo.merge(solicitud.getTipo(), 1, Integer::sum);
        }
        return porTipo;
    }

    private int contarVencidas(List<Solicitud> todas) {
        int vencidas = 0;
        LocalDateTime ahora = LocalDateTime.now(clock);
        for (Solicitud solicitud : todas) {
            if (solicitud.estaVencida(ahora)) {
                vencidas++;
            }
        }
        return vencidas;
    }
}
