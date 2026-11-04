package ar.edu.utn.tup.solicitudes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.tup.solicitudes.dominio.excepciones.ReglaDeNegocioException;

class SolicitudTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 11, 1, 10, 0, 0);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 2, 12, 0, 0);

    private static Responsable responsable(boolean activo) {
        Responsable responsable = new Responsable();
        responsable.setId(1L);
        responsable.setNombre("Carla Gómez");
        responsable.setActivo(activo);
        return responsable;
    }

    private static Solicitud pendiente(TipoSolicitud tipo, Prioridad prioridad) {
        NuevaSolicitud nueva = new NuevaSolicitud("L-1001", tipo, "Descripción de prueba válida", prioridad);
        return Solicitud.registrar(nueva, new Estudiante(), CREACION);
    }

    private static Solicitud pendiente() {
        return pendiente(TipoSolicitud.OTRO, null);
    }

    private static Solicitud enAnalisis() {
        Solicitud solicitud = pendiente();
        solicitud.asignarResponsable(responsable(true), AHORA);
        return solicitud;
    }

    private static Solicitud resuelta() {
        Solicitud solicitud = enAnalisis();
        solicitud.cambiarEstado(EstadoSolicitud.RESUELTA, AHORA);
        return solicitud;
    }

    @Test
    @DisplayName("registrar: estado PENDIENTE, prioridad del tipo, vencimiento = creación + plazo (RN-04..06)")
    void registrarConPrioridadPorDefecto() {
        Solicitud solicitud = pendiente(TipoSolicitud.EQUIVALENCIA, null);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(solicitud.getPrioridad()).isEqualTo(Prioridad.BAJA);
        assertThat(solicitud.getFechaCreacion()).isEqualTo(CREACION);
        assertThat(solicitud.getFechaVencimiento()).isEqualTo(CREACION.plusDays(15));
        assertThat(solicitud.getFechaActualizacion()).isNull();
        assertThat(solicitud.tieneResponsable()).isFalse();
    }

    @Test
    @DisplayName("registrar: la prioridad ALTA informada reduce el plazo a la mitad")
    void registrarConPrioridadAlta() {
        Solicitud solicitud = pendiente(TipoSolicitud.CAMBIO_COMISION, Prioridad.ALTA);
        assertThat(solicitud.getPrioridad()).isEqualTo(Prioridad.ALTA);
        assertThat(solicitud.getFechaVencimiento()).isEqualTo(CREACION.plusDays(1));
    }

    @Test
    @DisplayName("cambiarEstado: transición válida actualiza estado y fechaActualizacion (RN-12)")
    void cambiarEstadoValido() {
        Solicitud solicitud = pendiente();
        solicitud.cambiarEstado(EstadoSolicitud.CANCELADA, AHORA);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.CANCELADA);
        assertThat(solicitud.getFechaActualizacion()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("cambiarEstado: transición inválida lanza ReglaDeNegocioException y no modifica nada")
    void cambiarEstadoInvalido() {
        Solicitud solicitud = pendiente();
        assertThatThrownBy(() -> solicitud.cambiarEstado(EstadoSolicitud.RESUELTA, AHORA))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Transición inválida: PENDIENTE -> RESUELTA");
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(solicitud.getFechaActualizacion()).isNull();
    }

    @Test
    @DisplayName("cambiarEstado: resolver exige responsable (RN-08)")
    void cambiarEstadoSinResponsable() {
        Solicitud solicitud = pendiente();
        solicitud.cambiarEstado(EstadoSolicitud.EN_ANALISIS, AHORA);
        assertThatThrownBy(() -> solicitud.cambiarEstado(EstadoSolicitud.RESUELTA, AHORA))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("requiere un responsable asignado");
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.EN_ANALISIS);
    }

    @Test
    @DisplayName("cambiarEstado: con responsable puede pasar a RESUELTA y a RECHAZADA")
    void cambiarEstadoConResponsable() {
        assertThat(resuelta().getEstado()).isEqualTo(EstadoSolicitud.RESUELTA);
        Solicitud solicitud = enAnalisis();
        solicitud.cambiarEstado(EstadoSolicitud.RECHAZADA, AHORA);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
    }

    @Test
    @DisplayName("verificarQueAdmiteResponsable: falla si está cerrada y pasa si está abierta (RN-09)")
    void verificarQueAdmiteResponsable() {
        assertThatCode(() -> pendiente().verificarQueAdmiteResponsable()).doesNotThrowAnyException();
        assertThatCode(() -> enAnalisis().verificarQueAdmiteResponsable()).doesNotThrowAnyException();
        assertThatThrownBy(() -> resuelta().verificarQueAdmiteResponsable())
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("La solicitud está cerrada");
    }

    @Test
    @DisplayName("asignarResponsable: desde PENDIENTE pasa a EN_ANALISIS (RN-10)")
    void asignarDesdePendiente() {
        Solicitud solicitud = pendiente();
        Responsable asignado = responsable(true);
        solicitud.asignarResponsable(asignado, AHORA);
        assertThat(solicitud.getResponsable()).isSameAs(asignado);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.EN_ANALISIS);
        assertThat(solicitud.getFechaActualizacion()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("asignarResponsable: desde EN_ANALISIS reasigna sin cambiar el estado")
    void reasignarEnAnalisis() {
        Solicitud solicitud = enAnalisis();
        Responsable otro = responsable(true);
        solicitud.asignarResponsable(otro, AHORA.plusHours(1));
        assertThat(solicitud.getResponsable()).isSameAs(otro);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.EN_ANALISIS);
        assertThat(solicitud.getFechaActualizacion()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("asignarResponsable: responsable inactivo o solicitud cerrada lanzan 409 y no modifican (RN-09)")
    void asignarRechazado() {
        Solicitud abierta = pendiente();
        assertThatThrownBy(() -> abierta.asignarResponsable(responsable(false), AHORA))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("no está activo");
        assertThat(abierta.tieneResponsable()).isFalse();
        assertThat(abierta.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);

        Solicitud cerrada = resuelta();
        Responsable original = cerrada.getResponsable();
        assertThatThrownBy(() -> cerrada.asignarResponsable(responsable(true), AHORA))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("cerrada");
        assertThat(cerrada.getResponsable()).isSameAs(original);
    }

    @Test
    @DisplayName("estaVencida: con fecha fija, solo si venció y no está cerrada (RN-11)")
    void estaVencida() {
        Solicitud solicitud = pendiente(TipoSolicitud.CERTIFICADO_ALUMNO_REGULAR, null);
        LocalDateTime vencimiento = solicitud.getFechaVencimiento();
        assertThat(vencimiento).isEqualTo(CREACION.plusDays(2));
        assertThat(solicitud.estaVencida(vencimiento.minusSeconds(1))).isFalse();
        assertThat(solicitud.estaVencida(vencimiento)).isFalse();
        assertThat(solicitud.estaVencida(vencimiento.plusSeconds(1))).isTrue();
        assertThat(resuelta().estaVencida(LocalDateTime.of(2030, 1, 1, 0, 0))).isFalse();
    }
}
