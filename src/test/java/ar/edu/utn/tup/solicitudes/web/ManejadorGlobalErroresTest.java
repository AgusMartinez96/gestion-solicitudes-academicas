package ar.edu.utn.tup.solicitudes.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.tup.solicitudes.dominio.excepciones.DatoInvalidoException;
import ar.edu.utn.tup.solicitudes.dominio.excepciones.RecursoNoEncontradoException;
import ar.edu.utn.tup.solicitudes.dominio.excepciones.ReglaDeNegocioException;

/**
 * Prueba el {@link ManejadorGlobalErrores} de forma aislada (sin contexto de Spring Boot): un controller de
 * prueba lanza cada tipo de excepción y se verifica el código HTTP y el cuerpo {@code ErrorResponse}.
 */
class ManejadorGlobalErroresTest {

    private static final String DETALLE_INTERNO = "tabla solicitud, columna secreta, SQL: select * from x";

    @RestController
    static class ControladorQueFalla {

        @GetMapping("/falla/inesperada")
        String inesperada() {
            throw new IllegalStateException(DETALLE_INTERNO);
        }

        @GetMapping("/falla/dato")
        String dato() {
            throw new DatoInvalidoException("Estado inválido: FOO");
        }

        @GetMapping("/falla/noencontrado")
        String noEncontrado() {
            throw new RecursoNoEncontradoException("Solicitud no encontrada: 99");
        }

        @GetMapping("/falla/regla")
        String regla() {
            throw new ReglaDeNegocioException("La solicitud está cerrada: 3");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-04T13:00:00.789Z"), ZoneOffset.UTC);
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorQueFalla())
                .setControllerAdvice(new ManejadorGlobalErrores(reloj)).build();
    }

    @Test
    @DisplayName("Un error inesperado responde 500 con ErrorResponse JSON y mensaje genérico, sin detalles internos")
    void errorInesperado() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/falla/inesperada"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"))
                .andExpect(jsonPath("$.timestamp").value("2026-11-04T13:00:00"))
                .andExpect(content().string(not(containsString("secreta"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("SQL"))))
                .andReturn();
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("at ar.edu");
    }

    @Test
    @DisplayName("DatoInvalidoException responde 400 con el mensaje de la excepción")
    void datoInvalido() throws Exception {
        mockMvc.perform(get("/falla/dato")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").value("Estado inválido: FOO"));
    }

    @Test
    @DisplayName("RecursoNoEncontradoException responde 404 con el mensaje de la excepción")
    void noEncontrado() throws Exception {
        mockMvc.perform(get("/falla/noencontrado")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("Solicitud no encontrada: 99"));
    }

    @Test
    @DisplayName("ReglaDeNegocioException responde 409 con el mensaje de la excepción")
    void reglaDeNegocio() throws Exception {
        mockMvc.perform(get("/falla/regla")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensaje").value("La solicitud está cerrada: 3"));
    }
}
