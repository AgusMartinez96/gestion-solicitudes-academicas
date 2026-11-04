package ar.edu.utn.tup.solicitudes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.edu.utn.tup.solicitudes.dominio.excepciones.DatoInvalidoException;

class TipoSolicitudTest {

    @ParameterizedTest(name = "{0}: {1} días, prioridad {2}")
    @DisplayName("Plazo y prioridad por defecto de cada tipo (RN-04, RN-05)")
    @CsvSource({ "CERTIFICADO_ALUMNO_REGULAR,2,MEDIA", "REVISION_EXAMEN,5,ALTA", "EQUIVALENCIA,15,BAJA",
            "CAMBIO_COMISION,3,MEDIA", "OTRO,7,BAJA" })
    void plazoYPrioridad(TipoSolicitud tipo, int plazo, Prioridad prioridad) {
        assertThat(tipo.plazoDias()).isEqualTo(plazo);
        assertThat(tipo.prioridadPorDefecto()).isEqualTo(prioridad);
    }

    @Test
    @DisplayName("parsear devuelve el tipo o lanza DatoInvalidoException con el mensaje del contrato")
    void parsear() {
        assertThat(TipoSolicitud.parsear("EQUIVALENCIA")).isEqualTo(TipoSolicitud.EQUIVALENCIA);
        assertThatThrownBy(() -> TipoSolicitud.parsear("BECA")).isInstanceOf(DatoInvalidoException.class)
                .hasMessage("Tipo de solicitud inválido: BECA");
        assertThatThrownBy(() -> TipoSolicitud.parsear("otro")).isInstanceOf(DatoInvalidoException.class);
    }
}
