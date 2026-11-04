# Evolución profesional de un sistema existente: Gestión de Solicitudes Académicas

**Metodología de Sistemas II**
**Tecnicatura Universitaria en Programación a Distancia – UTN**

| | |
|---|---|
| **Trabajo** | Trabajo Práctico Integrador |
| **Título** | Evolución profesional de un sistema existente: Gestión de Solicitudes Académicas |
| **Autor** | Agustín Martínez (entrega individual) |
| **Comisión** | 5 |
| **Docente** | Diego Lobos |
| **Fecha** | 4 de noviembre de 2026 |

> Borrador integrado. Las marcas 📸 indican dónde va una captura que ya existe en `docs/evidencias/`. Los pasajes `_(…)_` los completa el autor. Todas las cifras provienen de los documentos de `docs/tpi/` y de los reportes del repositorio.

---

## 1. Resumen ejecutivo

Se tomó un sistema de gestión de solicitudes académicas (API REST en Java 21 y Spring Boot 4.1.2) que funciona correctamente desde afuera pero está construido "con apuro": un controller de 433 líneas que concentra siete responsabilidades. Se diagnosticaron 12 code smells y 56 violaciones de PMD. Antes de modificar nada se escribieron 15 tests de caracterización sobre el contrato HTTP; luego se aplicaron 9 refactorings (uno por commit, con los tests en verde en cada paso) y se hizo un code review del Pull Request con 14 observaciones. El resultado es la v2, con el mismo contrato HTTP: PMD bajó de 56 a 9 violaciones, el controller de 433 a 100 líneas (unas 70 de lógica), los tests de 16 a 105 y la cobertura de instrucciones de 82,2 % a 95,5 %. El trabajo no mide cantidad de funcionalidad sino calidad interna, y demuestra con pruebas antes y después que el comportamiento externo no cambió.

---

## 2. Sistema base

### 2.1 Dominio y problema

En una facultad, los estudiantes hacen trámites (certificados, revisiones de examen, equivalencias, cambios de comisión) por mail o en ventanilla. No hay trazabilidad: no se sabe en qué estado está cada pedido, quién lo atiende ni cuáles están vencidos. El **Sistema de Gestión de Solicitudes Académicas** centraliza esas solicitudes: permite registrarlas, clasificarlas, asignarlas a un responsable, cambiar su estado, consultarlas y obtener un reporte.

**Fuera de alcance:** login y roles, frontend propio, notificaciones reales por mail (se simula con una línea de consola) y adjuntos.

### 2.2 Funcionalidades

| RF | Requerimiento | Endpoint | Éxito | Errores |
|---|---|---|---|---|
| RF-01 | Registrar una solicitud | `POST /api/solicitudes` | 201 | 400, 404 |
| RF-02 | Consultar por id | `GET /api/solicitudes/{id}` | 200 | 404 |
| RF-03 | Buscar por estado, tipo, prioridad y/o legajo | `GET /api/solicitudes?...` | 200 | 400 |
| RF-04 | Cambiar el estado respetando transiciones | `PATCH /api/solicitudes/{id}/estado` | 200 | 400, 404, 409 |
| RF-05 | Asignar un responsable | `PATCH /api/solicitudes/{id}/responsable` | 200 | 404, 409 |
| RF-06 | Reporte resumen | `GET /api/reportes/resumen` | 200 | — |
| RF-07 | Listar estudiantes y responsables | `GET /api/estudiantes`, `GET /api/responsables` | 200 | — |

Reglas destacadas (RN-01 a RN-12 en `docs/01-PRD.md`): las transiciones válidas son `PENDIENTE → EN_ANALISIS | CANCELADA` y `EN_ANALISIS → RESUELTA | RECHAZADA`; resolver o rechazar exige responsable; asignar un responsable a una solicitud `PENDIENTE` la pasa a `EN_ANALISIS`; el plazo depende del tipo (2, 5, 15, 3 y 7 días) y la prioridad `ALTA` lo divide por 2 con un mínimo de 1 día.

### 2.3 Tecnología

Java 21 · Spring Boot 4.1.2 · Spring Data JPA con H2 en memoria · springdoc-openapi 3.2.0 · Maven con wrapper · JUnit 5, MockMvc, AssertJ y Mockito · JaCoCo 0.8.16 · PMD 7.17.0 (maven-pmd-plugin 3.28.0).

### 2.4 Evidencia de funcionamiento de la v1

📸 [CAPTURA: docs/evidencias/antes/capturas/01-swagger-endpoints.png] — Swagger UI con los 8 endpoints.
📸 [CAPTURA: docs/evidencias/antes/capturas/02-post-201.png] — registro de una solicitud (201).
📸 [CAPTURA: docs/evidencias/antes/capturas/04-reporte.png] — reporte resumen.
📸 [CAPTURA: docs/evidencias/antes/capturas/05-notificacion-consola.png] — notificación simulada por consola.

Estado de la v1 (tag `estado-inicial`): 16 tests en verde (15 de caracterización + `contextLoads`), cobertura JaCoCo de 82,2 % en instrucciones y 60,5 % en ramas, y 56 violaciones PMD.

---

## 3. Calidad externa vs. calidad interna

Se usa el modelo de calidad de producto de **ISO/IEC 25010**.

**Calidad externa (lo que percibe quien usa la API).** La v1 implementa los 8 endpoints y las 12 reglas de negocio, devuelve los códigos y mensajes del contrato y se explora desde Swagger UI sin herramientas extra. Valoración: **alta**. Una reserva: el cuerpo de los errores es texto plano y no hay contrato tipado de errores.

**Calidad interna (mantenibilidad).**

| Subcaracterística | Problema concreto en la v1 | Smells |
|---|---|---|
| Modularidad | Un controller concentra validación, reglas, acceso a datos, mapeo a JSON, notificación y reporte: 433 líneas, 7 responsabilidades, complejidad ciclomática de clase 119. | S01, S08, S11 |
| Reusabilidad | La lógica solo existe dentro de métodos de un controller HTTP, atada a `ResponseEntity` y a `Map`. | S01, S06 |
| Analizabilidad | Complejidad ciclomática de hasta 32 y NPath de 41.515; `catch (Exception)` oculta la causa; nombres que no revelan intención. | S02, S09, S10 |
| Modificabilidad | La regla "cerrada" repetida 7 veces; el mapeo a JSON en 5 bloques; agregar un tipo obliga a tocar 6 lugares. | S03, S04, S07 |
| Capacidad de ser probado | No se puede probar la lógica sin Spring y sin HTTP; el reloj y el notificador no se pueden sustituir. | S11, S01 |

**Ejemplo de costo del cambio: agregar el tipo `BECA`.** En la v1 hay que modificar **6 lugares en 2 archivos** y el compilador no avisa de ninguno; **dos fallan en silencio** (el plazo cae en el `default` de 7 días y la prioridad en `BAJA`) y otro falla lejos del origen (el reporte lanza un `NullPointerException` que el `catch` convierte en un 500). En la v2 es **una constante en el enum `TipoSolicitud`**.

| | Calidad externa | Calidad interna |
|---|---|---|
| v1 | Alta: completa y correcta | Baja: 12 smells, 56 violaciones PMD, cambios costosos y riesgosos |

Una cobertura de 82 % es alta pero engañosa: mide que el código se ejecuta, no que esté bien diseñado.

---

## 4. Deuda técnica y code smells

### 4.1 Los 12 smells

| ID | Smell | Dónde aparece en la v1 | Impacto | Prioridad | Refactoring |
|---|---|---|---|---|---|
| S01 | God Class / Large Class | `SolicitudController` (433 líneas) | Cualquier cambio toca la misma clase | Alta | R06 |
| S02 | Long Method | `crear` (77 líneas), `listar` (64), `reporte` (43) | Difícil de leer, probar y modificar | Alta | R01 |
| S03 | Duplicated Code | Mapeo a JSON en 5 endpoints; chequeo "cerrada" en 7 lugares | Un cambio de campo obliga a editar 5 sitios | Alta | R01 |
| S04 | Primitive Obsession / Magic Strings | `estado`, `tipo`, `prioridad` como `String` | Errores de tipeo silenciosos | Alta | R02 |
| S05 | Long Parameter List + Data Clumps | `armarSolicitud(7 params)`; `Map<String,Object>` como cuerpo | Llamadas confusas | Media | R05 |
| S06 | Feature Envy | `puedeCambiar(Solicitud, String)` | La regla vive lejos de sus datos | Alta | R03 |
| S07 | Switch repetidos (viola OCP) | `switch(tipo)` en `Utils` + `if/else` en el controller | Agregar un tipo obliga a modificar varios `switch` | Media | R04 |
| S08 | Anemic Domain Model | `Solicitud` solo getters y setters | Reglas dispersas; entidad inconsistente | Media | R03 |
| S09 | Nombres pobres + Magic Numbers | `s`, `aux`, `lista2`, `procesar`; `10`, `500` | Hay que leer el cuerpo para entender | Media | R09 |
| S10 | Manejo de errores deficiente | `catch (Exception)` + códigos `"404|..."` en el texto | Oculta bugs; acopla HTTP a texto libre | Alta | R07 |
| S11 | Acoplamiento fuerte / viola DIP | `new NotificadorConsola()`, `LocalDateTime.now()`, `Utils` estático, `@Autowired` en campos | No se puede testear ni sustituir | Media | R08 |
| S12 | Dead Code / `System.out` | Bloque comentado; `System.out.println("DEBUG")` | Ruido | Baja | R09 |

### 4.2 Los más importantes, con evidencia

**S01 — God Class.** Una sola clase resuelve siete responsabilidades (entrada, reglas, acceso a datos, armado de JSON, notificación, reporte y traducción de errores).

```java
@RestController
@Transactional
public class SolicitudController {
    @Autowired private SolicitudRepository solicitudRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private ResponsableRepository responsableRepository;
    @PostMapping("/api/solicitudes")
    public ResponseEntity<Object> crear(@RequestBody Map<String, Object> body) {
```

📸 [CAPTURA: docs/evidencias/antes/capturas/01-swagger-endpoints.png] — el reporte aparece dentro de `solicitud-controller`.

*Por qué PMD no lo detecta:* la regla `GodClass` exige WMC ≥ 47, ATFD > 5 y TCC < 1/3 a la vez. El controller cumple las dos primeras (WMC ≥ 100) pero su TCC queda apenas por encima de 1/3; la heurística no falla, mide otra cosa (cohesión por uso compartido de atributos, no responsabilidades de negocio). Por eso el diagnóstico se sostuvo con métricas de tamaño y complejidad.

**S03 — Código duplicado.** El mismo bloque de mapeo a JSON, copiado 5 veces, con el cálculo de `vencida` incluido:

```java
Map<String, Object> res = new LinkedHashMap<>();
res.put("id", s.getId());
...
res.put("vencida", !(s.getEstado().equals("RESUELTA") || s.getEstado().equals("RECHAZADA")
        || s.getEstado().equals("CANCELADA")) && s.getFechaVencimiento().isBefore(LocalDateTime.now()));
```

**S04 — Primitive Obsession.** Tres atributos `String` y 49 literales de estado, 30 de tipo y 11 de prioridad escritos a mano:

```java
if (tipo == null || !(tipo.equals("CERTIFICADO_ALUMNO_REGULAR") || tipo.equals("REVISION_EXAMEN")
        || tipo.equals("EQUIVALENCIA") || tipo.equals("CAMBIO_COMISION") || tipo.equals("OTRO"))) {
```

**S10 — Manejo de errores.** El código HTTP viaja dentro del texto del mensaje y se recupera parseando el prefijo:

```java
throw new RuntimeException("404|Estudiante no encontrado: " + legajo);
...
} catch (Exception e) { return procesar(e); }
```

📸 [CAPTURA: docs/evidencias/antes/capturas/03-error-409.png] — error 409 en texto plano.
📸 [CAPTURA: docs/evidencias/antes/capturas/06-example-value-vacio.png] — el "Example Value" de Swagger sale `{}` por devolver `Map<String, Object>` (S05).

### 4.3 Priorización y clasificación de la deuda

Matriz impacto × esfuerzo: *ganancias rápidas* (impacto alto, esfuerzo bajo) S03 y S06; impacto alto y esfuerzo medio S02, S04 y S10; S01 es de impacto alto y esfuerzo alto.

Según el cuadrante de Fowler, la deuda de la v1 es **imprudente y deliberada** ("no tenemos tiempo para diseñar"): se tomó un camino rápido sin plan para pagarla. Aclaración honesta: esa deuda fue **construida a propósito con fines didácticos** para poder diagnosticarla y refactorizarla.

### 4.4 Qué ve y qué no ve el análisis estático

PMD informó **56 violaciones** (28 `ShortVariable`, 6 `CyclomaticComplexity`, 6 `AvoidCatchingGenericException`, 5 `NcssCount`, 3 `SystemPrintln`, 3 `NPathComplexity`, 3 `CognitiveComplexity`, 1 `ExcessiveParameterList`, 1 `AvoidLiteralsInIfCondition`). **Seis de los doce smells** (S03, S04, S06, S07, S08, S11) no los detecta ninguna de las reglas configuradas: las herramientas complementan, pero no reemplazan, el análisis del diseño.

---

## 5. SOLID y Clean Code

Detalle completo, con diagramas de clases antes/después en Mermaid, en [`04-solid-clean-code.md`](04-solid-clean-code.md).

| Principio | Problema en v1 | Mejora en v2 | Refactoring | Grado |
|---|---|---|---|---|
| **SRP** | Controller con 7 responsabilidades | Controller, Service, Mapper, Reporte y Manejador de errores separados | R06 (+ O-4, O-5) | Fuerte |
| **OCP** | Agregar un tipo = tocar 6 lugares | Un tipo nuevo es una constante en `TipoSolicitud` | R02, R04 | Fuerte |
| **LSP** | Sin abstracciones | `Notificador` sustituible (`NotificadorLog`, `NotificadorFake`) | R08 | Acotado |
| **ISP** | Sin interfaces propias | `Notificador` con un solo método | R08 | Acotado |
| **DIP** | `new NotificadorConsola()`, `LocalDateTime.now()`, `Utils` estático | Depende de `Notificador` y de `Clock`, inyectados por constructor | R08 | Fuerte |

Clean Code: nombres que revelan intención y constantes (R09); funciones chicas (R01: el método más largo pasó de 77 a 15 líneas); sin duplicación (R01, R03, R05-B); pocos parámetros y contratos tipados (R05); manejo de errores centralizado (R07); cohesión (R03, O-1, O-6); acoplamiento (R08); límites entre componentes (R06-B, O-2, ADR-005, ADR-006).

**Honestidad técnica:** LSP e ISP se cumplen pero con poca superficie (una interfaz de un método); no se inventaron abstracciones para mostrar más SOLID.

---

## 6. Refactorings: antes y después

### 6.1 Los nueve refactorings

| ID | Técnica (Fowler) | Smells | Principio | Commit |
|---|---|---|---|---|
| R01 | Extract Method | S02, S03 | Clean Code | `9232424` |
| R02 | Replace Type Code with Enum | S04 | Clean Code | `fb7220c` |
| R03 | Move Method | S06, S08 | Cohesión / *Tell, Don't Ask* | `43c190e` |
| R04 | Replace Conditional with Polymorphism | S07 | OCP | `bbe0574` |
| R05 | Introduce Parameter Object (A: entradas · B: respuestas) | S05 | Clean Code | `f4d44c2`, `28f2ef5` |
| R06 | Extract Class (A) + paquetes por capas (B) | S01 | SRP | `1c8d5af`, `3b32287` |
| R07 | Replace Error Code with Exception + manejo centralizado | S10 | Clean Code, SRP | `7ab83de` |
| R08 | Introduce Interface + Dependency Injection | S11 | DIP | `d3c1ebb` |
| R09 | Rename + Replace Magic Number + Remove Dead Code | S09, S12 | Clean Code | `bd1b115` |

Protocolo por refactoring: cambio mínimo → `.\mvnw.cmd test` (16 tests en verde) → ficha con antes/después → un commit. Los tests de `src/test/.../api/` **no se modificaron**: si un refactoring rompía un test, se corregía el código de producción.

📸 [CAPTURA: docs/evidencias/despues/capturas/02-tests-intactos.png] — `git log --stat` de los tests de caracterización: un único commit, el de `estado-inicial`.

### 6.2 R01 — Extract Method (S02, S03)

*Motivación:* `crear`, `listar`, `reporte`, `cambiarEstado` y `asignarResponsable` eran métodos de 40 a 77 líneas; el bloque de mapeo estaba copiado 5 veces y la condición "cerrada" 7 veces.

**Antes (`estado-inicial`):** `crear` de 77 líneas con todo en línea.

```java
if (descripcion == null || descripcion.length() < 10 || descripcion.length() > 500) {
    throw new RuntimeException("400|La descripción debe tener entre 10 y 500 caracteres");
}
...
if (prioridad == null) {
    if (tipo.equals("REVISION_EXAMEN")) {
        prioridad = "ALTA";
    } else if (tipo.equals("CAMBIO_COMISION") || tipo.equals("CERTIFICADO_ALUMNO_REGULAR")) {
```

**Después:** `crear` de 30 líneas que solo delega, con `aMap` y `estaCerrada` únicos.

```java
validarDescripcion(descripcion);
validarTipo(tipo);
prioridad = resolverPrioridad(tipo, prioridad);
Utils.validar(descripcion, tipo, prioridad, legajo);
Estudiante est = buscarEstudiante(legajo);
...
int dias = calcularPlazoDias(tipo, prioridad);
Solicitud s = armarSolicitud(legajo, tipo, descripcion, prioridad, ahora, dias, est);
s = solicitudRepository.save(s);
return ResponseEntity.status(HttpStatus.CREATED).body(aMap(s));
```

Resultado: el controller pasó de 433 a 401 líneas y el cuerpo de los endpoints quedó entre 8 y 30. Pasos seguros: extraer primero lo que no depende de nada; copiar el bloque de mapeo tal cual (mismo orden de claves, mismo `null`); conservar el orden descripción → tipo → prioridad → legajo. Tests: 16/16.

### 6.3 R03 — Move Method (S06, S08)

*Motivación:* la regla de transiciones solo usaba datos de la solicitud pero vivía en el controller; la entidad era un contenedor de getters y setters.

**Antes:**

```java
private boolean puedeCambiar(Solicitud s, String nuevo) {
    String actual = s.getEstado();
    if (actual.equals("PENDIENTE")) {
        if (nuevo.equals("EN_ANALISIS") || nuevo.equals("CANCELADA")) {
```

```java
s.setResponsable(resp);
if (s.getEstado().equals("PENDIENTE")) {
    s.setEstado("EN_ANALISIS");
    s.setFechaActualizacion(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
}
```

**Después:** la máquina de estados vive en el enum y la entidad decide sobre sus propios datos.

```java
public boolean puedeTransicionarA(EstadoSolicitud destino) {
    return switch (this) {
        case PENDIENTE -> destino == EN_ANALISIS || destino == CANCELADA;
        case EN_ANALISIS -> destino == RESUELTA || destino == RECHAZADA;
        case RESUELTA, RECHAZADA, CANCELADA -> false;
    };
}
```

```java
if (!s.getEstado().puedeTransicionarA(nuevo)) { ... }
if (nuevo.requiereResponsable() && !s.tieneResponsable()) { ... }
s.cambiarEstado(nuevo, ahoraTruncado());
...
s.asignarResponsable(resp, ahoraTruncado());
```

`SolicitudController` pasó de 401 a 362 líneas. Decisión de alcance: los métodos de dominio todavía no lanzaban excepciones (hacerlo antes de R07 habría acoplado la entidad al protocolo HTTP). Tests: 16/16.

### 6.4 R05 — Introduce Parameter Object (S05)

*Motivación:* `armarSolicitud` recibía 7 parámetros, `filtrar` recibía 4 filtros sueltos y los endpoints de escritura recibían un `Map<String, Object>` casteado a mano.

**Antes:**

```java
public ResponseEntity<Object> crear(@RequestBody Map<String, Object> body) {
    String legajo = (String) body.get("legajo");
...
armarSolicitud(String legajo, TipoSolicitud tipo, String descripcion, Prioridad prioridad,
               LocalDateTime creacion, int dias, Estudiante est)
```

**Después:** `RegistrarSolicitudRequest` (record), `NuevaSolicitud` (record, calcula `prioridadEfectiva()` y `plazoDias()`) y `FiltroSolicitudes` (record con `coincideCon`).

```java
validarDescripcion(request.descripcion());
TipoSolicitud tipo = parsearTipo(request.tipo());
Prioridad prioridad = request.prioridad() == null ? null : parsearPrioridad(request.prioridad());
NuevaSolicitud nueva = new NuevaSolicitud(request.legajo(), tipo, request.descripcion(), prioridad);
Estudiante est = buscarEstudiante(nueva.legajo());
...
Solicitud s = armarSolicitud(nueva, est, ahora);   // 3 parámetros (antes 7)
```

*Cómo se preservó el contrato:* los records de request usan `String` para `tipo`, `prioridad` y `estado`; si fueran enums, un valor inválido fallaría en la deserialización con un error genérico en lugar del 400 con el mensaje del contrato. En la **parte B** se tiparon las respuestas (`SolicitudResponse` y otras); el JSON se verificó **idéntico** con una batería de 25 llamadas HTTP comparada con `diff` (0 diferencias). Tests: 16/16.

📸 [CAPTURA: docs/evidencias/despues/capturas/06-example-value-tipado.png] — el "Example Value" ya no es `{}`.

### 6.5 Los demás, en una línea

- **R02:** enums persistidos como texto. Hallazgo: Hibernate 7 generaba columnas `enum` nativas; se forzó `varchar` con `@JdbcTypeCode(SqlTypes.VARCHAR)`.
- **R04:** `TipoSolicitud` conoce su plazo y su prioridad por defecto; desaparecen los `switch` sobre el tipo.
- **R06:** `SolicitudService`, `ReporteService`, `SolicitudMapper` y `ReporteController`; paquetes por capas. Hallazgo: `LazyInitializationException` invisible para los tests (ver §7.3).
- **R07:** excepciones propias y un único `@RestControllerAdvice`; el cuerpo de error pasa a JSON (único cambio externo intencional).
- **R08:** `Notificador` + `NotificadorLog`, `Clock` inyectado, inyección por constructor, se elimina `util/`.
- **R09:** nombres, constantes, sin código comentado ni `System.out`.

---

## 7. Pruebas y verificación

Detalle en [`06-pruebas-verificacion.md`](06-pruebas-verificacion.md).

### 7.1 Estrategia

1. **Caracterización (caja negra):** CP-01..CP-15 por MockMvc, escritos sobre la v1 y sin modificar. Son la red de seguridad del refactoring.
2. **Unitarias sin Spring:** reglas del dominio, `SolicitudService` con `Clock.fixed` y un `NotificadorFake` (el beneficio concreto de DIP), y `ManejadorGlobalErrores`.
3. **Integración no transaccional:** servidor real en puerto aleatorio y cliente HTTP real, con base H2 propia.

| Tipo | Tests |
|---|---|
| Caracterización (HTTP, MockMvc) | 15 |
| Contexto | 1 |
| Unitarios de dominio (`EstadoSolicitudTest` 36, `SolicitudTest` 11, `PrioridadTest` 10, `TipoSolicitudTest` 6) | 63 |
| Unitarios de aplicación (`SolicitudServiceTest`) | 11 |
| Unitarios de web (`ManejadorGlobalErroresTest`) | 4 |
| Integración no transaccional | 11 |
| **Total** | **105** (v1: 16) |

### 7.2 Resultado de los casos CP-01..CP-15

El mismo archivo de tests se ejecutó sobre ambas versiones: **15/15 en `estado-inicial` y 15/15 en `main`**. Los 15 casos cubren: alta con plazo y prioridad (CP-01, CP-02), validaciones 400 y 404 (CP-03 a CP-05), consulta (CP-06, CP-07), filtros (CP-08), cambios de estado y sus errores (CP-09 a CP-12), asignación de responsable y sus errores (CP-13, CP-14) y reporte (CP-15). Tabla completa en `docs/pruebas/casos-de-prueba.md` (columnas v1 y v2).

### 7.3 El hallazgo del `@EntityGraph`

Los tests de caracterización son `@Transactional`: la sesión de JPA queda abierta y **no pueden detectar** un `LazyInitializationException`. Durante R06 el defecto apareció solo al llamar por HTTP. Se agregó la prueba no transaccional y se comprobó quitando temporalmente el `@EntityGraph`:

| Suite | Con `@EntityGraph` | Sin `@EntityGraph` |
|---|---|---|
| Caracterización | 15/15 | **15/15** (no lo detecta) |
| Integración no transaccional | 11/11 | **7/11** (4 fallan con `LazyInitializationException`) |

Decisión registrada en [ADR-006](decisiones/ADR-006-entitygraph-y-tests-no-transaccionales.md).

### 7.4 Métricas antes / después

| Métrica | v1 | v2 |
|---|---|---|
| Violaciones PMD | 56 | 9 (todas `ShortVariable` sobre `id`) |
| Líneas de `SolicitudController` | 433 | 100 (≈70 de lógica y 30 de anotaciones `@ApiResponse`) |
| Método más largo | 77 líneas | 15 líneas |
| Tipos de producción | 11 | 39 |
| Tests | 16 | 105 |
| Cobertura de instrucciones | 82,2 % | 95,5 % |
| Cobertura de ramas | 60,5 % | 86,1 % |
| Cobertura de líneas | 81,1 % | 93,9 % |

Las coberturas de la v2 se midieron con `.\mvnw.cmd clean test jacoco:report`. Con solo los tests de caracterización, la v2 tiene 86,1 % de instrucciones y 74,8 % de ramas.

### 7.5 Cambios externos intencionales

El contrato HTTP (rutas, métodos, campos, códigos y reglas) es idéntico. Las únicas diferencias están en el cuerpo de los errores:

| | v1 | v2 |
|---|---|---|
| `Content-Type` | `text/plain;charset=UTF-8` | `application/json;charset=UTF-8` |
| 409 | `Error: Transición inválida: CANCELADA -> EN_ANALISIS` | `{"status":409,"error":"Conflict","mensaje":"Transición inválida: CANCELADA -> EN_ANALISIS","timestamp":"2026-11-04T10:29:24"}` |

Además, tras el code review (O-9) tres errores del framework tienen mensaje en español: `Parámetro inválido: id`, `El cuerpo de la solicitud no es válido`, `Recurso no encontrado` (antes `Failed to convert 'id' with value: 'abc'`, `Failed to read request` y `Not Found`). El código HTTP se mantiene.

📸 [CAPTURA: docs/evidencias/despues/capturas/03-error-409-json.png] — error 409 en JSON.
📸 [CAPTURA: docs/evidencias/despues/capturas/01-swagger-endpoints.png] — Swagger de la v2 con el reporte en su propio controller.

---

## 8. Code review

### 8.1 Proceso

Se abrió el **Pull Request #1** (`refactor/solicitudes` → `main`, "Refactoring v2: SOLID, Clean Code y manejo de errores") y se aplicó un checklist de siete áreas (contrato, diseño/SOLID, Clean Code, manejo de errores, tests, datos y seguridad, Git y documentación) más cinco criterios de aceptación del PR. La revisión fue **asistida por IA** y las decisiones de aceptar, diferir o rechazar fueron **del autor**. El merge se hizo como *merge commit* (no squash) para conservar un commit por refactoring.

📸 [CAPTURA: docs/evidencias/despues/capturas/07-pull-request-review.png] — el PR con las observaciones.
📸 [CAPTURA: docs/evidencias/despues/capturas/08-decisiones-review.png] — decisiones del autor publicadas en el PR.
📸 [CAPTURA: docs/evidencias/despues/capturas/09-pr-merged.png] — PR #1 mergeado.

### 8.2 Las 14 observaciones y su resolución

Resultado: 0 bloqueantes, 3 mayores, 8 menores, 3 sugerencias → **11 aceptadas y corregidas**, **1 diferida** y **2 rechazadas**.

| ID | Sev. | Tema | Decisión | Commit |
|---|---|---|---|---|
| O-1 | mayor | Asimetría de validación entre `cambiarEstado` y `asignarResponsable` | Aceptada: reglas al dominio, orden 409 → 404 → 409 intacto | `6abbe72` |
| O-2 | mayor | Regla RN-02 en la capa web y parseo duplicado | Aceptada: único punto de validación | `1dc11fc` |
| O-3 | mayor | `spring-boot-starter-validation` sin uso | Aceptada (opción a): se quita y se registra ADR-005 | `792183e` |
| O-4 | menor | `SoporteController` salta la capa de aplicación | Aceptada: `ConsultaService` | `326eed0` |
| O-5 | menor | `vencida` calculada en la web, un reloj por solicitud | Aceptada: `SolicitudDetalle` con una sola hora | `d6e16c8` |
| O-6 | menor | Setters públicos que saltean invariantes | Aceptada: `Solicitud.registrar` y sin setters | `da59dbf` |
| O-7 | menor | `@EntityGraph` global también en el reporte | Aceptada: `findDetalleById`, `findAllConDetalle` | `ef61fed` |
| O-8 | menor | Consultas en transacciones de lectura-escritura | Aceptada: `readOnly = true` | `8fb3a98` |
| O-9 | menor | Mensajes de framework en inglés | Aceptada | `c2aebc4` |
| O-10 | menor | OpenAPI con fechas distintas a las reales y sin errores | Aceptada | `d4f805e` |
| O-11 | menor | Sin tests del manejador de errores ni de carga diferida | **Diferida** y resuelta en la Fase 6 | `a93d6b9` |
| O-12 | sugerencia | `listar` con 4 parámetros | **Rechazada**: son el contrato HTTP; el filtro interno ya es un Parameter Object | — |
| O-13 | sugerencia | Descripción de solo espacios es válida | **Rechazada**: `strip()` cambiaría el comportamiento heredado de la v1 | — |
| O-14 | sugerencia | Metadatos vacíos del `pom.xml` | Aceptada | `baad12e` |

Tras cada corrección se corrió `.\mvnw.cmd test` y una batería de 35 llamadas HTTP comparada con la línea base: sin diferencias, salvo el texto de los tres mensajes de O-9.

### 8.3 Criterios de aceptación del PR

| Criterio | Resultado |
|---|---|
| 15 tests de caracterización en verde, sin modificar | ✅ |
| Sin violaciones PMD de `GodClass`, `ExcessiveParameterList` ni `AvoidCatchingGenericException` | ✅ (0, 0 y 0) |
| Ningún método de producción con más de 20 líneas | ✅ (máximo 15) |
| Observaciones bloqueantes y mayores resueltas | ✅ |
| `refactorings.md` completo con hashes | ✅ |

Checklist completo en [`07-code-review-checklist.md`](07-code-review-checklist.md).

---

## 9. Conclusiones

- La v1 era correcta por fuera y cara de cambiar por dentro; la v2 mantiene el contrato y baja ese costo (agregar un tipo pasó de 6 lugares a una constante).
- La red de seguridad (caracterización) se construyó **antes** de refactorizar y no se tocó: eso sostiene la afirmación de que el comportamiento externo no cambió.
- Hallazgos reales: columnas `enum` nativas de Hibernate 7 (R02); `LazyInitializationException` invisible para los tests transaccionales (R06); cobertura de JaCoCo inflada sin `clean` (97,0 % medido vs. 95,5 % real); `GodClass` de PMD que no dispara por TCC; 6 de 12 smells no detectables por herramientas.
- Límites: sin autenticación, H2 en memoria, filtrado en memoria, descripción de solo espacios válida (O-13) y O-12 rechazada.
- Mejoras futuras: filtrado y paginación en la base, autenticación y roles, persistencia real, integración continua y CPD.

Detalle en [`08-conclusiones.md`](08-conclusiones.md).

**Conclusión individual:** _(la escribe el autor)_
**Justificación de la entrega individual:** _(completa el autor)_

---

## 10. Declaración de uso de IA

| Herramienta | Finalidad | Grado de intervención |
|---|---|---|
| Claude (chat, claude.ai) | Planificación del TPI, armado de la documentación de partida (PRD, TRD, diseño, plan) y de los prompts por fase. | Alto en la planificación; revisada y ajustada por el autor. |
| Claude Code (extensión de VS Code) | Generación de la v1 y de los tests de caracterización, de los refactorings, de los tests nuevos y de borradores de documentación; revisión asistida del Pull Request. | Alto en la generación de código; el autor revisó, ejecutó y verificó cada cambio. |

**Criterio de validación del autor:** ejecutar los tests después de cada cambio; leer el diff de cada commit; contrastar cada smell y cada refactoring con la bibliografía de la materia; tomar él mismo las decisiones del code review; y corregir lo que no correspondía. Los prompts usados están en orden en [`docs/ia/registro-prompts.md`](../ia/registro-prompts.md). _(El autor completa en ese registro qué revisó o corrigió en cada fase.)_

---

## 11. Enlaces

- **Repositorio:** https://github.com/AgusMartinez96/gestion-solicitudes-academicas
- **Pull Request #1:** https://github.com/AgusMartinez96/gestion-solicitudes-academicas/pull/1
- **Video:** {{URL_VIDEO}}
- **Tags:** `estado-inicial` (v1), `etapa-diagnostico`, `etapa-refactoring`, `etapa-pruebas`, `etapa-docs` y `estado-refactorizado` (v2).

## 12. Anexos

| Anexo | Contenido |
|---|---|
| A | Reportes de la v1: `docs/evidencias/antes/` (surefire, JaCoCo, PMD, capturas) |
| B | Reportes de la v2: `docs/evidencias/despues/` (surefire, JaCoCo, PMD, capturas) |
| C | Diagnóstico completo: `docs/tpi/01`, `02` y `03` |
| D | Fichas de refactoring: `docs/tpi/refactorings.md` |
| E | SOLID y Clean Code con diagramas: `docs/tpi/04-solid-clean-code.md` |
| F | Pruebas y casos CP: `docs/tpi/06-pruebas-verificacion.md`, `docs/pruebas/casos-de-prueba.md` |
| G | Code review: `docs/tpi/07-code-review-checklist.md`, `docs/tpi/pr-descripcion.md` |
| H | Decisiones técnicas: `docs/tpi/decisiones/` (ADR-001 a ADR-006) |
| I | Registro de prompts de IA: `docs/ia/registro-prompts.md` |

Lista exacta de lo que va en el ZIP: [`docs/anexo-zip.md`](../anexo-zip.md).
