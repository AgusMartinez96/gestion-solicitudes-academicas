package ar.edu.utn.tup.solicitudes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.edu.utn.tup.solicitudes.dominio.excepciones.DatoInvalidoException;

class PrioridadTest {

    @ParameterizedTest(name = "{0}: {1} días -> {2}")
    @DisplayName("ajustarPlazo: ALTA divide a la mitad con mínimo de 1 día; BAJA y MEDIA no cambian (RN-06)")
    @CsvSource({ "ALTA,5,2", "ALTA,15,7", "ALTA,2,1", "ALTA,1,1", "ALTA,0,1", "MEDIA,5,5", "MEDIA,1,1",
            "BAJA,15,15", "BAJA,7,7" })
    void ajustarPlazo(Prioridad prioridad, int dias, int esperado) {
        assertThat(prioridad.ajustarPlazo(dias)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("parsear devuelve la prioridad o lanza DatoInvalidoException con el mensaje del contrato")
    void parsear() {
        assertThat(Prioridad.parsear("ALTA")).isEqualTo(Prioridad.ALTA);
        assertThatThrownBy(() -> Prioridad.parsear("URGENTE")).isInstanceOf(DatoInvalidoException.class)
                .hasMessage("Prioridad inválida: URGENTE");
    }
}
