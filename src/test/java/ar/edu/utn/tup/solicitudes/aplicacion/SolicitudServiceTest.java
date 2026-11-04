package ar.edu.utn.tup.solicitudes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ar.edu.utn.tup.solicitudes.dominio.EstadoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.Estudiante;
import ar.edu.utn.tup.solicitudes.dominio.NuevaSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.Responsable;
import ar.edu.utn.tup.solicitudes.dominio.Solicitud;
import ar.edu.utn.tup.solicitudes.dominio.TipoSolicitud;
import ar.edu.utn.tup.solicitudes.dominio.excepciones.DatoInvalidoException;
import ar.edu.utn.tup.solicitudes.dominio.excepciones.RecursoNoEncontradoException;
import ar.edu.utn.tup.solicitudes.dominio.excepciones.ReglaDeNegocioException;
import ar.edu.utn.tup.solicitudes.persistencia.EstudianteRepository;
import ar.edu.utn.tup.solicitudes.persistencia.ResponsableRepository;
import ar.edu.utn.tup.solicitudes.persistencia.SolicitudRepository;

/**
 * Prueba el service sin Spring ni base de datos. Gracias a DIP (R08) el {@link Notificador} y el {@link Clock}
 * se reemplazan por un fake y un reloj fijo: la prueba no depende de la consola ni de la hora real.
 */
class SolicitudServiceTest {

    private static final Instant INSTANTE = Instant.parse("2026-11-04T13:00:00.789Z");
    private static final LocalDateTime AHORA_TRUNCADO = LocalDateTime.of(2026, 11, 4, 13, 0, 0);

    private final NotificadorFake notificador = new NotificadorFake();
    private final SolicitudRepository solicitudRepository = mock(SolicitudRepository.class);
    private final EstudianteRepository estudianteRepository = mock(EstudianteRepository.class);
    private final ResponsableRepository responsableRepository = mock(ResponsableRepository.class);
    private SolicitudService service;

    private static final class NotificadorFake implements Notificador {
        private final List<Solicitud> notificadas = new ArrayList<>();

        @Override
        public void notificarCierre(Solicitud solicitud) {
            notificadas.add(solicitud);
        }
    }

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(INSTANTE, ZoneOffset.UTC);
        service = new SolicitudService(solicitudRepository, estudianteRepository, responsableRepository,
                notificador, reloj);
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private Responsable responsable(long id, boolean activo) {
        Responsable responsable = new Responsable();
        responsable.setId(id);
        responsable.setActivo(activo);
        when(responsableRepository.findById(id)).thenReturn(Optional.of(responsable));
        return responsable;
    }

    private Solicitud solicitudGuardada(long id, EstadoSolicitud destino, boolean conResponsable) {
        NuevaSolicitud nueva = new NuevaSolicitud("L-1001", TipoSolicitud.OTRO, "Descripción de prueba válida",
                null);
        Solicitud solicitud = Solicitud.registrar(nueva, new Estudiante(), AHORA_TRUNCADO.minusDays(1));
        if (conResponsable) {
            solicitud.asignarResponsable(responsable(1, true), AHORA_TRUNCADO.minusHours(1));
        }
        if (destino != EstadoSolicitud.PENDIENTE && destino != EstadoSolicitud.EN_ANALISIS) {
            solicitud.cambiarEstado(destino, AHORA_TRUNCADO.minusMinutes(1));
        }
        when(solicitudRepository.findDetalleById(id)).thenReturn(Optional.of(solicitud));
        return solicitud;
    }

    @ParameterizedTest(name = "pasar a {0} notifica una vez")
    @DisplayName("cambiarEstado notifica cuando la solicitud pasa a RESUELTA o RECHAZADA")
    @EnumSource(value = EstadoSolicitud.class, names = { "RESUELTA", "RECHAZADA" })
    void notificaAlResolver(EstadoSolicitud destino) {
        solicitudGuardada(7, EstadoSolicitud.EN_ANALISIS, true);
        SolicitudDetalle detalle = service.cambiarEstado(7L, destino.name());
        assertThat(detalle.solicitud().getEstado()).isEqualTo(destino);
        assertThat(notificador.notificadas).containsExactly(detalle.solicitud());
    }

    @Test
    @DisplayName("cambiarEstado no notifica al pasar a EN_ANALISIS ni a CANCELADA")
    void noNotificaEnOtrosCasos() {
        solicitudGuardada(1, EstadoSolicitud.PENDIENTE, false);
        service.cambiarEstado(1L, "EN_ANALISIS");
        solicitudGuardada(2, EstadoSolicitud.PENDIENTE, false);
        service.cambiarEstado(2L, "CANCELADA");
        assertThat(notificador.notificadas).isEmpty();
    }

    @Test
    @DisplayName("cambiarEstado no notifica si la transición es inválida o falta el responsable")
    void noNotificaSiFalla() {
        solicitudGuardada(3, EstadoSolicitud.PENDIENTE, false);
        assertThatThrownBy(() -> service.cambiarEstado(3L, "RESUELTA")).isInstanceOf(ReglaDeNegocioException.class);
        solicitudGuardada(4, EstadoSolicitud.EN_ANALISIS, false);
        assertThatThrownBy(() -> service.cambiarEstado(4L, "RESUELTA")).isInstanceOf(ReglaDeNegocioException.class);
        assertThat(notificador.notificadas).isEmpty();
    }

    @Test
    @DisplayName("cambiarEstado: 404 de la solicitud antes que 400 del estado inválido")
    void ordenDeValidacionAlCambiarEstado() {
        when(solicitudRepository.findDetalleById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cambiarEstado(99L, "FOO")).isInstanceOf(RecursoNoEncontradoException.class);
        solicitudGuardada(5, EstadoSolicitud.PENDIENTE, false);
        assertThatThrownBy(() -> service.cambiarEstado(5L, "FOO")).isInstanceOf(DatoInvalidoException.class)
                .hasMessage("Estado inválido: FOO");
    }

    @Test
    @DisplayName("fechaActualizacion usa el Clock fijo, truncado a segundos")
    void usaElRelojInyectado() {
        solicitudGuardada(6, EstadoSolicitud.PENDIENTE, false);
        SolicitudDetalle detalle = service.cambiarEstado(6L, "EN_ANALISIS");
        assertThat(detalle.solicitud().getFechaActualizacion()).isEqualTo(AHORA_TRUNCADO);
    }

    @Test
    @DisplayName("vencida se calcula con la hora del Clock fijo (RN-11)")
    void vencidaConRelojFijo() {
        Solicitud solicitud = solicitudGuardada(8, EstadoSolicitud.PENDIENTE, false);
        assertThat(solicitud.getFechaVencimiento()).isEqualTo(AHORA_TRUNCADO.minusDays(1).plusDays(7));
        assertThat(service.obtener(8L).vencida()).isFalse();

        Solicitud vieja = Solicitud.registrar(
                new NuevaSolicitud("L-1001", TipoSolicitud.OTRO, "Descripción de prueba válida", null),
                new Estudiante(), AHORA_TRUNCADO.minusDays(30));
        when(solicitudRepository.findDetalleById(9L)).thenReturn(Optional.of(vieja));
        assertThat(service.obtener(9L).vencida()).isTrue();
    }

    @Test
    @DisplayName("asignarResponsable: solicitud cerrada (409) se detecta antes de buscar el responsable (404)")
    void ordenDeValidacionAlAsignar() {
        solicitudGuardada(10, EstadoSolicitud.RESUELTA, true);
        when(responsableRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.asignarResponsable(10L, 99L)).isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("cerrada");
    }

    @Test
    @DisplayName("asignarResponsable: responsable inexistente (404), nulo (404) o inactivo (409)")
    void asignarResponsableInvalido() {
        solicitudGuardada(11, EstadoSolicitud.PENDIENTE, false);
        when(responsableRepository.findById(99L)).thenReturn(Optional.empty());
        responsable(3, false);
        assertThatThrownBy(() -> service.asignarResponsable(11L, 99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.asignarResponsable(11L, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.asignarResponsable(11L, 3L)).isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("no está activo");
    }

    @Test
    @DisplayName("asignarResponsable: responsable activo pasa la solicitud PENDIENTE a EN_ANALISIS (RN-10)")
    void asignarResponsableValido() {
        solicitudGuardada(12, EstadoSolicitud.PENDIENTE, false);
        responsable(2, true);
        SolicitudDetalle detalle = service.asignarResponsable(12L, 2L);
        assertThat(detalle.solicitud().getEstado()).isEqualTo(EstadoSolicitud.EN_ANALISIS);
        assertThat(detalle.solicitud().getResponsable().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("registrar: valida el pedido antes de buscar al estudiante y usa el Clock fijo")
    void registrar() {
        when(estudianteRepository.findByLegajo("L-1001")).thenReturn(Optional.of(new Estudiante()));
        PedidoRegistro valido = new PedidoRegistro("L-1001", "REVISION_EXAMEN", "Descripción de prueba válida",
                null);
        SolicitudDetalle detalle = service.registrar(valido);
        assertThat(detalle.solicitud().getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(detalle.solicitud().getFechaCreacion()).isEqualTo(AHORA_TRUNCADO);
        assertThat(detalle.solicitud().getFechaVencimiento()).isEqualTo(AHORA_TRUNCADO.plusDays(2));

        PedidoRegistro corto = new PedidoRegistro("L-9999", "OTRO", "corta", null);
        assertThatThrownBy(() -> service.registrar(corto)).isInstanceOf(DatoInvalidoException.class);
        PedidoRegistro sinEstudiante = new PedidoRegistro("L-9999", "OTRO", "Descripción de prueba válida", null);
        assertThatThrownBy(() -> service.registrar(sinEstudiante))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
