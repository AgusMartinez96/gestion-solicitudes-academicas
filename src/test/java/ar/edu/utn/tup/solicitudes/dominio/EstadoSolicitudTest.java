package ar.edu.utn.tup.solicitudes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import ar.edu.utn.tup.solicitudes.dominio.excepciones.DatoInvalidoException;

class EstadoSolicitudTest {

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @DisplayName("Tabla completa de transiciones (RN-07)")
    @CsvSource({
            "PENDIENTE,PENDIENTE,false", "PENDIENTE,EN_ANALISIS,true", "PENDIENTE,RESUELTA,false",
            "PENDIENTE,RECHAZADA,false", "PENDIENTE,CANCELADA,true",
            "EN_ANALISIS,PENDIENTE,false", "EN_ANALISIS,EN_ANALISIS,false", "EN_ANALISIS,RESUELTA,true",
            "EN_ANALISIS,RECHAZADA,true", "EN_ANALISIS,CANCELADA,false",
            "RESUELTA,PENDIENTE,false", "RESUELTA,EN_ANALISIS,false", "RESUELTA,RESUELTA,false",
            "RESUELTA,RECHAZADA,false", "RESUELTA,CANCELADA,false",
            "RECHAZADA,PENDIENTE,false", "RECHAZADA,EN_ANALISIS,false", "RECHAZADA,RESUELTA,false",
            "RECHAZADA,RECHAZADA,false", "RECHAZADA,CANCELADA,false",
            "CANCELADA,PENDIENTE,false", "CANCELADA,EN_ANALISIS,false", "CANCELADA,RESUELTA,false",
            "CANCELADA,RECHAZADA,false", "CANCELADA,CANCELADA,false" })
    void transiciones(EstadoSolicitud origen, EstadoSolicitud destino, boolean permitida) {
        assertThat(origen.puedeTransicionarA(destino)).isEqualTo(permitida);
    }

    @ParameterizedTest
    @EnumSource(EstadoSolicitud.class)
    @DisplayName("Un estado cerrado es RESUELTA, RECHAZADA o CANCELADA")
    void esCerrado(EstadoSolicitud estado) {
        Set<EstadoSolicitud> cerrados = EnumSet.of(EstadoSolicitud.RESUELTA, EstadoSolicitud.RECHAZADA,
                EstadoSolicitud.CANCELADA);
        assertThat(estado.esCerrado()).isEqualTo(cerrados.contains(estado));
    }

    @ParameterizedTest
    @EnumSource(EstadoSolicitud.class)
    @DisplayName("Solo RESUELTA y RECHAZADA son resoluciones y exigen responsable (RN-08)")
    void resolucionYResponsable(EstadoSolicitud estado) {
        boolean esperado = estado == EstadoSolicitud.RESUELTA || estado == EstadoSolicitud.RECHAZADA;
        assertThat(estado.esResolucion()).isEqualTo(esperado);
        assertThat(estado.requiereResponsable()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("parsear devuelve el estado o lanza DatoInvalidoException con el mensaje del contrato")
    void parsear() {
        assertThat(EstadoSolicitud.parsear("RESUELTA")).isEqualTo(EstadoSolicitud.RESUELTA);
        assertThatThrownBy(() -> EstadoSolicitud.parsear("FOO")).isInstanceOf(DatoInvalidoException.class)
                .hasMessage("Estado inválido: FOO");
        assertThatThrownBy(() -> EstadoSolicitud.parsear(null)).isInstanceOf(DatoInvalidoException.class);
    }
}
