package ar.edu.utn.tup.solicitudes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SolicitudesApiCaracterizacionTest {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions enviarJson(MockHttpServletRequestBuilder request, String json) throws Exception {
        return mockMvc.perform(request
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8)
                .content(json.getBytes(StandardCharsets.UTF_8)));
    }

    private ResultActions crear(String json) throws Exception {
        return enviarJson(post("/api/solicitudes"), json);
    }

    private ResultActions cambiarEstado(long id, String json) throws Exception {
        return enviarJson(patch("/api/solicitudes/" + id + "/estado"), json);
    }

    private ResultActions asignarResponsable(long id, String json) throws Exception {
        return enviarJson(patch("/api/solicitudes/" + id + "/responsable"), json);
    }

    private String cuerpo(ResultActions resultado) throws Exception {
        MvcResult mvcResult = resultado.andReturn();
        return mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Duration plazoEntreCreacionYVencimiento(String cuerpoJson) {
        LocalDateTime creacion = LocalDateTime.parse(JsonPath.<String>read(cuerpoJson, "$.fechaCreacion"));
        LocalDateTime vencimiento = LocalDateTime.parse(JsonPath.<String>read(cuerpoJson, "$.fechaVencimiento"));
        return Duration.between(creacion, vencimiento);
    }

    @Test
    @DisplayName("CP-01 — Registrar CAMBIO_COMISION con prioridad MEDIA: 201, PENDIENTE y vence a los 3 días")
    void cp01RegistrarSolicitudConPrioridadInformada() throws Exception {
        ResultActions resultado = crear("""
                {"legajo":"L-1001","tipo":"CAMBIO_COMISION",
                 "descripcion":"Quiero cambiar a la comisión del turno noche","prioridad":"MEDIA"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.prioridad").value("MEDIA"))
                .andExpect(jsonPath("$.tipo").value("CAMBIO_COMISION"))
                .andExpect(jsonPath("$.estudiante.legajo").value("L-1001"))
                .andExpect(jsonPath("$.responsable").value(nullValue()));

        assertThat(plazoEntreCreacionYVencimiento(cuerpo(resultado))).isEqualTo(Duration.ofDays(3));
    }

    @Test
    @DisplayName("CP-02 — Registrar REVISION_EXAMEN sin prioridad: 201, prioridad ALTA y vence a los 2 días")
    void cp02RegistrarSolicitudConPrioridadPorDefecto() throws Exception {
        ResultActions resultado = crear("""
                {"legajo":"L-1002","tipo":"REVISION_EXAMEN",
                 "descripcion":"Solicito revisión del parcial de Programación III"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prioridad").value("ALTA"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        assertThat(plazoEntreCreacionYVencimiento(cuerpo(resultado))).isEqualTo(Duration.ofDays(2));
    }

    @Test
    @DisplayName("CP-03 — Descripción corta: 400 con el mensaje de longitud")
    void cp03DescripcionInvalida() throws Exception {
        crear("""
                {"legajo":"L-1001","tipo":"OTRO","descripcion":"corta"}""")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(
                        "La descripción debe tener entre 10 y 500 caracteres")));
    }

    @Test
    @DisplayName("CP-04 — Tipo inexistente (BECA): 400 con el mensaje de tipo inválido")
    void cp04TipoInvalido() throws Exception {
        crear("""
                {"legajo":"L-1001","tipo":"BECA","descripcion":"Quiero solicitar una beca de estudio"}""")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Tipo de solicitud inválido: BECA")));
    }

    @Test
    @DisplayName("CP-05 — Legajo inexistente: 404 con el mensaje de estudiante no encontrado")
    void cp05EstudianteInexistente() throws Exception {
        crear("""
                {"legajo":"L-9999","tipo":"OTRO","descripcion":"Consulta general sobre el calendario"}""")
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Estudiante no encontrado: L-9999")));
    }

    @Test
    @DisplayName("CP-06 — Consultar solicitud 2: 200, EN_ANALISIS, responsable Carla Gómez y fechas sin decimales")
    void cp06ObtenerSolicitudExistente() throws Exception {
        ResultActions resultado = mockMvc.perform(get("/api/solicitudes/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.estado").value("EN_ANALISIS"))
                .andExpect(jsonPath("$.responsable.nombre").value("Carla Gómez"))
                .andExpect(jsonPath("$.estudiante.legajo").value("L-1002"));

        String cuerpoJson = cuerpo(resultado);
        assertThat(JsonPath.<String>read(cuerpoJson, "$.fechaCreacion")).doesNotContain(".");
        assertThat(JsonPath.<String>read(cuerpoJson, "$.fechaVencimiento")).doesNotContain(".");
    }

    @Test
    @DisplayName("CP-07 — Consultar solicitud inexistente: 404 con el mensaje de solicitud no encontrada")
    void cp07ObtenerSolicitudInexistente() throws Exception {
        mockMvc.perform(get("/api/solicitudes/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Solicitud no encontrada: 99")));
    }

    @Test
    @DisplayName("CP-08 — Filtros: estado, estado+legajo y estado inválido")
    void cp08BuscarConFiltros() throws Exception {
        mockMvc.perform(get("/api/solicitudes").param("estado", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(1, 5)));

        mockMvc.perform(get("/api/solicitudes").param("estado", "PENDIENTE").param("legajo", "L-1003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(5)));

        mockMvc.perform(get("/api/solicitudes").param("estado", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Estado inválido: FOO")));
    }

    @Test
    @DisplayName("CP-09 — Cambiar la solicitud 5 a EN_ANALISIS: 200 y fechaActualizacion registrada")
    void cp09CambiarEstadoValido() throws Exception {
        cambiarEstado(5, """
                {"estado":"EN_ANALISIS"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_ANALISIS"))
                .andExpect(jsonPath("$.fechaActualizacion").value(not(nullValue())));
    }

    @Test
    @DisplayName("CP-10 — Transición inválida: 409 desde CANCELADA y 400 con estado FOO")
    void cp10TransicionInvalidaYEstadoInvalido() throws Exception {
        cambiarEstado(4, """
                {"estado":"EN_ANALISIS"}""")
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Transición inválida: CANCELADA -> EN_ANALISIS")));

        cambiarEstado(1, """
                {"estado":"FOO"}""")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Estado inválido: FOO")));
    }

    @Test
    @DisplayName("CP-11 — Resolver sin responsable asignado: 409")
    void cp11ResolverSinResponsable() throws Exception {
        cambiarEstado(1, """
                {"estado":"EN_ANALISIS"}""")
                .andExpect(status().isOk());

        cambiarEstado(1, """
                {"estado":"RESUELTA"}""")
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("requiere un responsable asignado")));
    }

    @Test
    @DisplayName("CP-12 — Resolver la solicitud 2 que ya tiene responsable: 200 y RESUELTA")
    void cp12ResolverConResponsable() throws Exception {
        cambiarEstado(2, """
                {"estado":"RESUELTA"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTA"));
    }

    @Test
    @DisplayName("CP-13 — Asignar responsable 1 a la solicitud 5: 200, pasa a EN_ANALISIS")
    void cp13AsignarResponsable() throws Exception {
        asignarResponsable(5, """
                {"responsableId":1}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_ANALISIS"))
                .andExpect(jsonPath("$.responsable.id").value(1));
    }

    @Test
    @DisplayName("CP-14 — Asignación inválida: responsable inactivo, solicitud cerrada y responsable inexistente")
    void cp14AsignarResponsableInvalido() throws Exception {
        asignarResponsable(5, """
                {"responsableId":3}""")
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("El responsable no está activo: 3")));

        asignarResponsable(3, """
                {"responsableId":1}""")
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("La solicitud está cerrada: 3")));

        asignarResponsable(5, """
                {"responsableId":99}""")
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Responsable no encontrado: 99")));
    }

    @Test
    @DisplayName("CP-15 — Reporte resumen con los datos semilla")
    void cp15ReporteResumen() throws Exception {
        mockMvc.perform(get("/api/reportes/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.porEstado.PENDIENTE").value(2))
                .andExpect(jsonPath("$.porEstado.EN_ANALISIS").value(1))
                .andExpect(jsonPath("$.porEstado.RESUELTA").value(1))
                .andExpect(jsonPath("$.porEstado.RECHAZADA").value(0))
                .andExpect(jsonPath("$.porEstado.CANCELADA").value(1))
                .andExpect(jsonPath("$.porTipo.CERTIFICADO_ALUMNO_REGULAR").value(1))
                .andExpect(jsonPath("$.porTipo.REVISION_EXAMEN").value(1))
                .andExpect(jsonPath("$.porTipo.EQUIVALENCIA").value(1))
                .andExpect(jsonPath("$.porTipo.CAMBIO_COMISION").value(1))
                .andExpect(jsonPath("$.porTipo.OTRO").value(1))
                .andExpect(jsonPath("$.vencidas").value(1));
    }
}
