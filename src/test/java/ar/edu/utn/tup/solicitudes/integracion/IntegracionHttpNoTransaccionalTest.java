package ar.edu.utn.tup.solicitudes.integracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import com.jayway.jsonpath.JsonPath;

/**
 * Prueba de integración NO transaccional (O-11 del code review): levanta el servidor real en un puerto
 * aleatorio y usa un cliente HTTP real. A diferencia de los tests de caracterización (MockMvc dentro de una
 * transacción que mantiene la sesión de JPA abierta), acá la sesión se cierra al terminar el service, igual
 * que en producción con {@code open-in-view=false}. Por eso detecta una {@code LazyInitializationException}
 * si alguien quita el {@code @EntityGraph} del repositorio (hallazgo de R06).
 *
 * <p>Usa una base H2 propia ({@code integracion}) para no compartir datos ni esquema con los demás tests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:integracion;DB_CLOSE_DELAY=-1" })
@DirtiesContext
class IntegracionHttpNoTransaccionalTest {

    private static final String SOLICITUD_VALIDA = """
            {"legajo":"L-1001","tipo":"EQUIVALENCIA","descripcion":"Pido equivalencia de materia aprobada","prioridad":"ALTA"}""";

    private final HttpClient cliente = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int puerto;

    private record Respuesta(int status, String contentType, String cuerpo) {
        <T> T leer(String jsonPath) {
            return JsonPath.read(cuerpo, jsonPath);
        }
    }

    private Respuesta enviar(String metodo, String ruta, String json) throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
        if (json == null) {
            peticion.method(metodo, BodyPublishers.noBody());
        } else {
            peticion.header("Content-Type", "application/json")
                    .method(metodo, BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        }
        HttpResponse<String> respuesta = cliente.send(peticion.build(),
                BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Respuesta(respuesta.statusCode(),
                respuesta.headers().firstValue("Content-Type").orElse(""), respuesta.body());
    }

    private int crearSolicitud() throws Exception {
        Respuesta creada = enviar("POST", "/api/solicitudes", SOLICITUD_VALIDA);
        assertThat(creada.status()).isEqualTo(201);
        return creada.leer("$.id");
    }

    private void assertErrorJson(Respuesta respuesta, int status, String mensaje) {
        assertThat(respuesta.status()).isEqualTo(status);
        assertThat(respuesta.contentType()).startsWith("application/json");
        assertThat(respuesta.<Integer>leer("$.status")).isEqualTo(status);
        assertThat(respuesta.<String>leer("$.mensaje")).contains(mensaje);
        assertThat(respuesta.<String>leer("$.timestamp")).isNotBlank();
    }

    @Test
    @DisplayName("POST válido: 201 con estudiante y responsable nulo, prioridad ALTA y fechas sin decimales")
    void crearSolicitudValida() throws Exception {
        Respuesta respuesta = enviar("POST", "/api/solicitudes", SOLICITUD_VALIDA);
        assertThat(respuesta.status()).isEqualTo(201);
        assertThat(respuesta.<String>leer("$.estado")).isEqualTo("PENDIENTE");
        assertThat(respuesta.<String>leer("$.prioridad")).isEqualTo("ALTA");
        assertThat(respuesta.<String>leer("$.estudiante.legajo")).isEqualTo("L-1001");
        assertThat(respuesta.<Object>leer("$.responsable")).isNull();
        assertThat(respuesta.<String>leer("$.fechaCreacion")).doesNotContain(".");
    }

    @Test
    @DisplayName("GET por id con responsable: carga estudiante y responsable fuera de la transacción (@EntityGraph)")
    void obtenerConEstudianteYResponsable() throws Exception {
        Respuesta respuesta = enviar("GET", "/api/solicitudes/2", null);
        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.<String>leer("$.estado")).isEqualTo("EN_ANALISIS");
        assertThat(respuesta.<String>leer("$.estudiante.nombre")).isEqualTo("Tomás Rivero");
        assertThat(respuesta.<String>leer("$.responsable.nombre")).isEqualTo("Carla Gómez");
        assertThat(respuesta.<Boolean>leer("$.vencida")).isFalse();
    }

    @Test
    @DisplayName("Listado y filtros: incluye responsables cargados y respeta el orden por id")
    void listar() throws Exception {
        Respuesta todas = enviar("GET", "/api/solicitudes", null);
        assertThat(todas.status()).isEqualTo(200);
        List<Integer> ids = todas.leer("$[*].id");
        assertThat(ids).hasSizeGreaterThanOrEqualTo(5).isSorted();
        assertThat(todas.<List<String>>leer("$[?(@.id==2)].responsable.nombre")).containsExactly("Carla Gómez");

        Respuesta pendientes = enviar("GET", "/api/solicitudes?estado=PENDIENTE&legajo=L-1003", null);
        assertThat(pendientes.status()).isEqualTo(200);
        assertThat(pendientes.<List<Integer>>leer("$[*].id")).contains(5);
    }

    @Test
    @DisplayName("Asignar responsable: PENDIENTE pasa a EN_ANALISIS y la respuesta trae el responsable")
    void asignarResponsable() throws Exception {
        int id = crearSolicitud();
        Respuesta respuesta = enviar("PATCH", "/api/solicitudes/" + id + "/responsable", "{\"responsableId\":1}");
        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.<String>leer("$.estado")).isEqualTo("EN_ANALISIS");
        assertThat(respuesta.<String>leer("$.responsable.nombre")).isEqualTo("Carla Gómez");
        assertThat(respuesta.<String>leer("$.fechaActualizacion")).isNotBlank();

        Respuesta releida = enviar("GET", "/api/solicitudes/" + id, null);
        assertThat(releida.<String>leer("$.responsable.nombre")).isEqualTo("Carla Gómez");
    }

    @Test
    @DisplayName("Cambiar estado: resolver una solicitud con responsable responde 200")
    void resolverConResponsable() throws Exception {
        int id = crearSolicitud();
        enviar("PATCH", "/api/solicitudes/" + id + "/responsable", "{\"responsableId\":2}");
        Respuesta respuesta = enviar("PATCH", "/api/solicitudes/" + id + "/estado", "{\"estado\":\"RESUELTA\"}");
        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.<String>leer("$.estado")).isEqualTo("RESUELTA");
        assertThat(respuesta.<String>leer("$.responsable.nombre")).isEqualTo("Diego Paz");
    }

    @Test
    @DisplayName("400: descripción corta, tipo inválido, estado inválido y filtro inválido, con cuerpo JSON")
    void errores400() throws Exception {
        assertErrorJson(enviar("POST", "/api/solicitudes",
                "{\"legajo\":\"L-1001\",\"tipo\":\"OTRO\",\"descripcion\":\"corta\"}"), 400,
                "La descripción debe tener entre 10 y 500 caracteres");
        assertErrorJson(enviar("POST", "/api/solicitudes",
                "{\"legajo\":\"L-1001\",\"tipo\":\"BECA\",\"descripcion\":\"Descripción suficientemente larga\"}"),
                400, "Tipo de solicitud inválido: BECA");
        assertErrorJson(enviar("PATCH", "/api/solicitudes/1/estado", "{\"estado\":\"FOO\"}"), 400,
                "Estado inválido: FOO");
        assertErrorJson(enviar("GET", "/api/solicitudes?estado=FOO", null), 400, "Estado inválido: FOO");
    }

    @Test
    @DisplayName("404: solicitud inexistente, estudiante inexistente y responsable inexistente")
    void errores404() throws Exception {
        assertErrorJson(enviar("GET", "/api/solicitudes/999", null), 404, "Solicitud no encontrada: 999");
        assertErrorJson(enviar("POST", "/api/solicitudes",
                "{\"legajo\":\"L-9999\",\"tipo\":\"OTRO\",\"descripcion\":\"Descripción suficientemente larga\"}"),
                404, "Estudiante no encontrado: L-9999");
        assertErrorJson(enviar("PATCH", "/api/solicitudes/5/responsable", "{\"responsableId\":99}"), 404,
                "Responsable no encontrado: 99");
    }

    @Test
    @DisplayName("409: transición inválida, falta de responsable, responsable inactivo y solicitud cerrada")
    void errores409() throws Exception {
        assertErrorJson(enviar("PATCH", "/api/solicitudes/4/estado", "{\"estado\":\"EN_ANALISIS\"}"), 409,
                "Transición inválida: CANCELADA -> EN_ANALISIS");
        int id = crearSolicitud();
        enviar("PATCH", "/api/solicitudes/" + id + "/estado", "{\"estado\":\"EN_ANALISIS\"}");
        assertErrorJson(enviar("PATCH", "/api/solicitudes/" + id + "/estado", "{\"estado\":\"RESUELTA\"}"), 409,
                "requiere un responsable asignado");
        assertErrorJson(enviar("PATCH", "/api/solicitudes/" + id + "/responsable", "{\"responsableId\":3}"), 409,
                "El responsable no está activo: 3");
        assertErrorJson(enviar("PATCH", "/api/solicitudes/3/responsable", "{\"responsableId\":1}"), 409,
                "La solicitud está cerrada: 3");
        assertErrorJson(enviar("PATCH", "/api/solicitudes/3/responsable", "{\"responsableId\":99}"), 409,
                "La solicitud está cerrada: 3");
    }

    @Test
    @DisplayName("Errores de framework: id no numérico, JSON malformado y ruta inexistente, en JSON y en español")
    void erroresDeFramework() throws Exception {
        assertErrorJson(enviar("GET", "/api/solicitudes/abc", null), 400, "Parámetro inválido: id");
        assertErrorJson(enviar("POST", "/api/solicitudes", "{malformado"), 400,
                "El cuerpo de la solicitud no es válido");
        assertErrorJson(enviar("GET", "/api/nada", null), 404, "Recurso no encontrado");
    }

    @Test
    @DisplayName("Reporte: total coherente con porEstado y porTipo, todas las claves presentes")
    void reporte() throws Exception {
        Respuesta respuesta = enviar("GET", "/api/reportes/resumen", null);
        assertThat(respuesta.status()).isEqualTo(200);
        int total = respuesta.leer("$.total");
        Map<String, Integer> porEstado = respuesta.leer("$.porEstado");
        Map<String, Integer> porTipo = respuesta.leer("$.porTipo");
        assertThat(porEstado).containsOnlyKeys("PENDIENTE", "EN_ANALISIS", "RESUELTA", "RECHAZADA", "CANCELADA");
        assertThat(porTipo).containsOnlyKeys("CERTIFICADO_ALUMNO_REGULAR", "REVISION_EXAMEN", "EQUIVALENCIA",
                "CAMBIO_COMISION", "OTRO");
        assertThat(porEstado.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(total);
        assertThat(porTipo.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(total);
        assertThat(respuesta.<Integer>leer("$.vencidas")).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Estudiantes y responsables: listados con sus campos")
    void listadosDeSoporte() throws Exception {
        Respuesta estudiantes = enviar("GET", "/api/estudiantes", null);
        assertThat(estudiantes.status()).isEqualTo(200);
        assertThat(estudiantes.<List<String>>leer("$[*].legajo")).contains("L-1001", "L-1002", "L-1003");
        Respuesta responsables = enviar("GET", "/api/responsables", null);
        assertThat(responsables.status()).isEqualTo(200);
        assertThat(responsables.<List<Boolean>>leer("$[*].activo")).contains(true, false);
    }
}
