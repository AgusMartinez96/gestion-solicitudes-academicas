# 07 — Code review: checklist y observaciones

- **Pull Request:** `refactor/solicitudes` → `main` ("Refactoring v2: SOLID, Clean Code y manejo de errores"). Descripción en [`pr-descripcion.md`](pr-descripcion.md); detalle de cada refactoring en [`refactorings.md`](refactorings.md).
- **Alcance de la revisión:** `git diff main...refactor/solicitudes` (16 commits; 40 archivos de producción cambiados, `src/test/` sin cambios respecto de `estado-inicial`).
- **Cómo se hizo:** se aplicó el checklist de la tabla de abajo sobre el código de la rama, con mediciones reproducibles (PMD, JaCoCo, conteo de longitud de métodos, `grep` de patrones prohibidos, batería HTTP de 25 llamadas) y lectura de cada clase de producción.

> **Aclaración sobre el origen de esta revisión.** Las observaciones fueron producidas por una **revisión asistida por IA** (Claude Code) y deben ser **validadas por el autor**. La decisión de **aceptar o rechazar** cada observación es del autor; las rechazadas se justifican en la sección 3. Ninguna observación fue corregida todavía.

---

## 1. Checklist

La columna **Resultado** se completa después de resolver las observaciones (✅ / ⚠️ / ❌). La columna **Observación** indica qué observaciones de la sección 2 se relacionan con el criterio.

### A. Contrato y funcionalidad

| # | Criterio | Cómo se verifica | Resultado | Observación | Acción |
|---|---|---|---|---|---|
| A1 | Rutas, métodos, JSON y códigos HTTP iguales a la v1 | Batería HTTP de 25 llamadas capturada antes y después de los refactorings con `diff` (`docs/tpi/refactorings.md`, R05-B, R07 y R08) y tests CP-01..CP-15 | | O-10 (la documentación OpenAPI no refleja el formato real de fechas) | |
| A2 | Tests de caracterización intactos y en verde | `git diff estado-inicial -- src/test` vacío; `.\mvnw.cmd test` → 16 tests; captura `docs/evidencias/despues/capturas/02-tests-intactos.png` | | | |
| A3 | El único cambio externo es el formato del cuerpo de error, documentado | Ficha de R07 con ejemplo antes/después y capturas `antes/03-error-409.png` y `despues/03-error-409-json.png` | | O-9 (mensajes de errores de framework en inglés) | |

### B. Diseño / SOLID

| # | Criterio | Cómo se verifica | Resultado | Observación | Acción |
|---|---|---|---|---|---|
| B1 | Una responsabilidad por clase | Lectura de las clases; `SolicitudController` 433 → 70 líneas; PMD sin `GodClass` ni `CyclomaticComplexity` | | O-6 (la entidad conserva setters públicos) | |
| B2 | Controllers sin lógica de negocio | Lectura de `web/` | | O-4 (`SoporteController` accede a repositorios), O-12 (`listar` arma el filtro) | |
| B3 | Reglas de negocio en el dominio | Lectura de `dominio/` y `aplicacion/` | | **O-1** (asimetría `cambiarEstado` / `asignarResponsable`), **O-2** (RN-02 validada en el mapper web), O-5 (`vencida` calculada en el mapper) | |
| B4 | Dependencias por constructor e interfaces | `grep` de `@Autowired` (0); `Notificador` como interfaz; `Clock` inyectado | | | |
| B5 | Sin `new` de colaboradores | `grep` de `new` sobre servicios y controllers | | | |

### C. Clean Code

| # | Criterio | Cómo se verifica | Resultado | Observación | Acción |
|---|---|---|---|---|---|
| C1 | Nombres que revelan la intención | PMD `ShortVariable`: 10 violaciones, todas `id` (JPA y contrato JSON) | | | |
| C2 | Métodos de 20 líneas como máximo | Conteo sobre 98 métodos de `src/main`: máximo 14 líneas | | | |
| C3 | Hasta 3 parámetros por método | Conteo de parámetros; PMD `ExcessiveParameterList` = 0 | | O-12 (`listar` recibe 4 `@RequestParam`) | |
| C4 | Sin números ni strings mágicos | Constantes `LONGITUD_*`, `DIVISOR_PLAZO_ALTA`, `PLAZO_MINIMO_DIAS`; `AvoidLiteralsInIfCondition` = 0 | | | |
| C5 | Sin código muerto ni comentarios redundantes | `grep` de `System.out` (0) y de bloques comentados (0); dependencia sin uso | | O-3 (`spring-boot-starter-validation` sin uso) | |

### D. Manejo de errores

| # | Criterio | Cómo se verifica | Resultado | Observación | Acción |
|---|---|---|---|---|---|
| D1 | Excepciones específicas | `dominio/excepciones/` (3 clases) | | | |
| D2 | Sin `catch (Exception)` | `grep` (0); PMD `AvoidCatchingGenericException` = 0 | | | |
| D3 | Un solo `@RestControllerAdvice` | `web/ManejadorGlobalErrores` | | | |
| D4 | Mensajes claros y consistentes | Contraste de los mensajes del contrato con los de errores de framework | | O-9 | |

### E. Tests

| # | Criterio | Cómo se verifica | Resultado | Observación | Acción |
|---|---|---|---|---|---|
| E1 | Tests unitarios del dominio | `src/test/.../dominio/` (planificado para la Fase 6) | | O-11 | |
| E2 | Nombres descriptivos | `@DisplayName("CP-xx — ...")` en los tests de caracterización | | | |
| E3 | Sin dependencia de la hora real | `Clock` inyectado en `src/main`; `grep` de `LocalDateTime.now()` sin reloj (0) | | O-5 | |

### F. Datos y seguridad

| # | Criterio | Cómo se verifica | Resultado | Observación | Acción |
|---|---|---|---|---|---|
| F1 | Solo datos sintéticos | `data.sql`: nombres, legajos y emails inventados (`*.example`) | | | |
| F2 | Sin secretos en el repositorio | Búsqueda de credenciales en `application.properties` y `pom.xml` (usuario `sa` sin contraseña de la H2 en memoria) | | | |

### G. Git y documentación

| # | Criterio | Cómo se verifica | Resultado | Observación | Acción |
|---|---|---|---|---|---|
| G1 | Un commit por refactoring | `git log main..refactor/solicitudes`: R01, R02, R03, R04, R05 (A y B), R06 (A y B), R07, R08, R09 | | | |
| G2 | Mensajes de commit claros | Conventional Commits en español (`refactor(R0x): ...`) | | | |
| G3 | `refactorings.md` completo y README actualizado | Fichas R01–R09 con antes/después, hash y tests; README (se completa en la Fase 7) | | O-14 (metadatos vacíos del `pom.xml`) | |

### Criterios de aceptación del PR

| # | Criterio | Cómo se verifica | Resultado | Observación | Acción |
|---|---|---|---|---|---|
| P1 | 15/15 tests de caracterización en verde, sin modificaciones | `.\mvnw.cmd test`; `git diff estado-inicial -- src/test` | | | |
| P2 | Sin violaciones PMD de `GodClass`, `ExcessiveParameterList` ni `AvoidCatchingGenericException` | `.\mvnw.cmd pmd:pmd` → `target\reports\pmd.html` (0, 0 y 0) | | | |
| P3 | Ningún método de producción con más de 20 líneas | Conteo sobre `src/main` (máximo 14) | | | |
| P4 | Observaciones bloqueantes y mayores resueltas | Sección 2: no hay observaciones bloqueantes; hay 3 mayores (O-1, O-2, O-3) | | O-1, O-2, O-3 | |
| P5 | `refactorings.md` completo con hashes | Fichas R01–R09 con hash de commit | | | |

---

## 2. Observaciones

Formato: `[Severidad: bloqueante / mayor / menor / sugerencia] archivo:línea — Observación objetiva — Por qué importa — Propuesta`.
Las líneas corresponden al commit `4aba570` (cabeza de la rama al momento de la revisión).

**Resumen:** 14 observaciones — 0 bloqueantes, 3 mayores, 8 menores, 3 sugerencias. No se encontró ningún defecto que rompa el contrato HTTP ni los tests.

| ID | Severidad | Ubicación | Tema | Decisión del autor |
|---|---|---|---|---|
| O-1 | mayor | `Solicitud.java:89-94`, `SolicitudService.java:75-83` | Asimetría de validación entre `cambiarEstado` y `asignarResponsable` | |
| O-2 | mayor | `SolicitudMapper.java:38-43, 105-111`; `SolicitudService.java:107-110` | Regla RN-02 en la capa web y parseo de estado duplicado | |
| O-3 | mayor | `pom.xml:46, 70`; `SolicitudMapper.java:105-111` | `spring-boot-starter-validation` declarado y exigido por el TRD, pero sin uso | |
| O-4 | menor | `SoporteController.java:16-34` | Controller que salta la capa de aplicación | |
| O-5 | menor | `SolicitudMapper.java:32-36, 70`; `ReporteService.java:57-65` | `vencida` calculada en la capa web, con una lectura de reloj por solicitud | |
| O-6 | menor | `Solicitud.java:100-142`; `SolicitudService.java:116-126` | Setters públicos que permiten saltear los invariantes | |
| O-7 | menor | `SolicitudRepository.java:13-19`; `ReporteService.java:30` | `@EntityGraph` global aplicado también al reporte | |
| O-8 | menor | `SolicitudService.java:25`; `ReporteService.java:18` | Consultas dentro de transacciones de lectura-escritura | |
| O-9 | menor | `ManejadorGlobalErrores.java:59-74` | Mensajes de errores de framework en inglés | |
| O-10 | menor | `SolicitudResponse.java:15-17`; `SolicitudController.java:34-69` | OpenAPI documenta un formato de fecha distinto al real y no documenta los errores | |
| O-11 | menor | `src/test/` | Sin tests para el manejador de errores ni para el caso de carga diferida | |
| O-12 | sugerencia | `SolicitudController.java:47-56` | `listar` con 4 parámetros y armado del filtro en el controller | |
| O-13 | sugerencia | `SolicitudMapper.java:105-111` | Una descripción de solo espacios es válida (comportamiento heredado de la v1) | |
| O-14 | sugerencia | `pom.xml:14-27` | Metadatos vacíos heredados de Spring Initializr | |

### Detalle

**O-1 · [Severidad: mayor] `dominio/Solicitud.java:89-94` y `aplicacion/SolicitudService.java:75-83`** — `cambiarEstado` protege sus invariantes dentro del dominio (`Solicitud.java:77-87` lanza `ReglaDeNegocioException` por transición inválida o falta de responsable), pero `asignarResponsable` no valida nada: es un simple mutador (`Solicitud.java:89-94`) y las reglas RN-09 viven en el service (`SolicitudService.java:77-79` para "solicitud cerrada" y `:101-103` para "responsable inactivo"). — **Por qué importa:** la entidad no garantiza su propia consistencia. Cualquier otro llamador de `Solicitud.asignarResponsable` (otro caso de uso, un test, un futuro endpoint) puede asignar un responsable inactivo a una solicitud `RESUELTA` sin ningún error; y el criterio B3 ("reglas en el dominio") se cumple a medias, con dos estilos distintos en la misma clase. — **Propuesta:** mover ambas reglas a `Solicitud.asignarResponsable` (y que lance `ReglaDeNegocioException`). Atención al orden del contrato (409 solicitud cerrada → 404 responsable inexistente → 409 responsable inactivo), que intercala una búsqueda entre dos reglas: para preservarlo, el service debe seguir comprobando "cerrada" antes de buscar el responsable (por ejemplo con un método de dominio `verificarQueAdmiteResponsable()`), y la entidad volver a validar ambas condiciones como invariante. La alternativa de buscar primero el responsable cambiaría la respuesta en el caso de doble error (solicitud cerrada **y** responsable inexistente: hoy 409, pasaría a 404) y requeriría decidirlo explícitamente, porque el contrato HTTP no puede cambiar.

**O-2 · [Severidad: mayor] `web/SolicitudMapper.java:38-43, 105-111` y `aplicacion/SolicitudService.java:107-110`** — La regla RN-02 (longitud de la descripción, con sus constantes `LONGITUD_MINIMA/MAXIMA_DESCRIPCION`) y el parseo de tipo y prioridad (RN-03/RN-04) se validan en `SolicitudMapper`, es decir, en la capa web; en cambio el estado se parsea en el service (`parsearEstado`, `SolicitudService.java:107-110`), con la misma excepción y el mismo mensaje que `SolicitudMapper.aEstado` (`:50-53`). Hay dos criterios de frontera y un parseo duplicado. — **Por qué importa:** una regla de negocio depende del canal de entrada: otro cliente del service (un job, un test, otro controller) no la hereda; y un cambio de mensaje o de regla exige tocar dos lugares sin que el compilador avise. Contradice B3 y la separación de responsabilidades de R06. — **Propuesta:** dejar un único punto de validación. Opción recomendada: que el service reciba valores crudos (`NuevaSolicitud` con `String`s) y valide en orden descripción → tipo → prioridad → legajo; o mover el parseo a los enums (`EstadoSolicitud.parsear(String)`, `TipoSolicitud.parsear(String)`, `Prioridad.parsear(String)`, lanzando `DatoInvalidoException`) y usar esos métodos desde el mapper y el service. En ambos casos hay que conservar el orden del contrato (y la verificación del 404 de la solicitud antes del 400 de estado en `cambiarEstado`).

**O-3 · [Severidad: mayor] `pom.xml:46` y `pom.xml:70`** — `spring-boot-starter-validation` (y `spring-boot-starter-validation-test`) están declarados y el TRD §1 dice que la v2 usa "Jakarta Bean Validation", pero no hay ninguna referencia a `jakarta.validation`, `@Valid`, `@Size` ni `@NotBlank` en `src/main` (0 coincidencias); la validación de entrada sigue siendo manual (`SolicitudMapper.java:105-111`). — **Por qué importa:** es un requisito documentado que no se cumplió, deja una dependencia muerta (checklist C5) y el informe afirmaría algo que el código no muestra. — **Propuesta:** hay dos caminos y conviene elegir uno de forma explícita. (a) Mantener la validación manual: quitar las dos dependencias del `pom.xml`, actualizar el TRD y registrar la decisión en un ADR (la razón técnica es válida: Bean Validation acumula errores y los reporta sin garantizar el orden descripción → tipo → prioridad → legajo ni los mensajes exactos del contrato, que son parte de los tests). (b) Adoptarla para la descripción (`@Size(min = 10, max = 500)` con el mensaje exacto) verificando que el orden y el cuerpo de error no cambien. Se recomienda (a) con ADR.

**O-4 · [Severidad: menor] `web/SoporteController.java:16-34`** — El controller inyecta `EstudianteRepository` y `ResponsableRepository` y llama a `findAll()` directamente; los otros dos controllers pasan por un service. — **Por qué importa:** rompe la dirección de dependencias web → aplicación → persistencia que se estableció en R06 y el criterio B2; cuando haya que filtrar o paginar, esa lógica nacerá en el controller. — **Propuesta:** un `ConsultaService` (o `EstudianteService` y `ResponsableService`) en `aplicacion/` que devuelva las entidades, y que el controller solo mapee.

**O-5 · [Severidad: menor] `web/SolicitudMapper.java:32-36, 70` y `aplicacion/ReporteService.java:57-65`** — El mapper recibe un `Clock` para calcular `vencida` (RN-11) en la capa web, y lo hace llamando a `LocalDateTime.now(clock)` **una vez por solicitud** en los listados; `ReporteService` calcula "vencidas" con otro `LocalDateTime.now(clock)`. — **Por qué importa:** una regla de negocio (RN-11) se evalúa en dos capas y en instantes distintos; en un listado largo dos filas pueden usar horas de referencia diferentes, y el mapper acumula una dependencia (el reloj) que no es suya (B3, E3). — **Propuesta:** que el service fije una única hora de referencia y la entregue junto con las solicitudes (`mapper.aResponse(solicitud, ahora)` o un resultado de aplicación con `vencida` ya resuelta), y que el mapper deje de depender de `Clock`.

**O-6 · [Severidad: menor] `dominio/Solicitud.java:100-142` y `aplicacion/SolicitudService.java:116-126`** — La entidad sigue exponiendo setters públicos para todos sus campos, incluidos `setEstado` (`Solicitud.java:132`), `setResponsable` y las fechas, y el service construye cada solicitud con siete llamadas a setters (`armarSolicitud`). — **Por qué importa:** `setEstado(...)` permite saltear `cambiarEstado()` y sus reglas; es lo que queda del smell S08 (modelo anémico) y hace que las invariantes dependan de la disciplina de quien llama. — **Propuesta:** un método de fábrica `Solicitud.registrar(...)` que fije `PENDIENTE` y calcule el vencimiento, y quitar los setters de `estado`, `responsable` y las fechas (JPA usa acceso por campo, no los necesita). Para no volver a tener cinco parámetros, la fábrica puede recibir `NuevaSolicitud` (movido a `dominio`), el `Estudiante` y la fecha de creación.

**O-7 · [Severidad: menor] `persistencia/SolicitudRepository.java:13-19` y `aplicacion/ReporteService.java:30`** — `@EntityGraph(attributePaths = { "estudiante", "responsable" })` se aplica a la sobrescritura de `findAll()` y `findById()`, por lo que *todo* llamador hace los `JOIN` a ambas asociaciones, incluido el reporte, que solo usa `estado`, `tipo` y las fechas. El motivo (evitar `LazyInitializationException` en el mapper, con `open-in-view=false`) no está explicado en el código, solo en la ficha de R06. — **Por qué importa:** costo oculto en una consulta que no lo necesita y una configuración difícil de entender para quien llegue después. — **Propuesta:** métodos con nombre explícito (`findDetalleById`, `findAllConDetalle`) para los casos que arman respuestas, `findAll()` normal para el reporte, y un comentario breve que explique el *porqué* del grafo.

**O-8 · [Severidad: menor] `aplicacion/SolicitudService.java:25` y `aplicacion/ReporteService.java:18`** — `@Transactional` está a nivel de clase, así que `obtener`, `buscar` y `resumen` (que solo leen) abren transacciones de lectura-escritura. — **Por qué importa:** pierden la optimización de transacciones de solo lectura (sin dirty checking ni flush) y no expresan la intención. — **Propuesta:** `@Transactional(readOnly = true)` en la clase de `ReporteService` y en los métodos de consulta de `SolicitudService`, y `@Transactional` solo en `registrar`, `cambiarEstado` y `asignarResponsable`.

**O-9 · [Severidad: menor] `web/ManejadorGlobalErrores.java:59-74`** — Para los errores de framework, `detalleSeguro` toma el texto del `ProblemDetail` de Spring, que está en inglés: `"Failed to read request"` (JSON malformado), `"Failed to convert 'id' with value: 'abc'"` (id no numérico) y `"Not Found"` (ruta inexistente), según la batería HTTP de R07. El resto de los mensajes del contrato está en español. — **Por qué importa:** criterio D4 ("mensajes claros y consistentes"); un cliente recibe errores en dos idiomas, y el texto de `Failed to convert ... value` revela el nombre interno del parámetro. — **Propuesta:** sobrescribir `handleHttpMessageNotReadable`, `handleTypeMismatch` y `handleNoResourceFoundException` con mensajes fijos en español (`"El cuerpo de la solicitud no es válido"`, `"Parámetro inválido: id"`, `"Recurso no encontrado"`), manteniendo el código HTTP.

**O-10 · [Severidad: menor] `web/dto/SolicitudResponse.java:15-17` y `web/SolicitudController.java:34-69`** — La captura `docs/evidencias/despues/capturas/06-example-value-tipado.png` muestra el "Example Value" con fechas `"2026-11-04T13:30:06.513Z"` (con zona `Z` y milisegundos), mientras que la API devuelve `"2026-11-04T10:29:24"` (sin zona ni decimales, como fija el contrato §2 y se ve en `03-error-409-json.png`). Además, el tipo de contenido figura como `*/*` y los códigos 400/404/409 aparecen como *Undocumented* (`03-error-409-json.png`). — **Por qué importa:** el principal beneficio de R05-B era tener un contrato tipado y documentado; si la documentación promete otro formato de fecha, un cliente generado a partir de OpenAPI parsearía mal las fechas. — **Propuesta:** anotar los componentes de fecha con `@Schema(type = "string", format = "date-time", example = "2026-11-04T10:29:24")` (o un `OpenApiCustomizer` global para `LocalDateTime`), declarar `produces = MediaType.APPLICATION_JSON_VALUE` en los mappings y documentar `ErrorResponse` para los 400/404/409 con `@ApiResponse`.

**O-11 · [Severidad: menor] `src/test/`** — No hay tests propios para `ManejadorGlobalErrores` (el camino del 500 y los errores de framework se verificaron solo a mano, ficha R07) ni un test que detecte `LazyInitializationException`: los tests de caracterización son `@Transactional`, mantienen la sesión abierta y, como se vio en R06, no pueden detectar el fallo (apareció recién por HTTP real). — **Por qué importa:** dos de los riesgos más serios que aparecieron durante el refactoring no tienen red de seguridad automática. — **Propuesta (Fase 6, fuera de `api/`):** un test no transaccional (`@SpringBootTest` con `MockMvc` sin `@Transactional`) que consulte una solicitud con responsable, y un test del manejador de errores que fuerce una excepción inesperada y verifique el cuerpo `500` sin detalles internos; además de los tests unitarios de dominio ya previstos.

**O-12 · [Severidad: sugerencia] `web/SolicitudController.java:47-56`** — `listar` recibe cuatro `@RequestParam` y arma el `FiltroSolicitudes` en el propio controller con tres condicionales ternarios. — **Por qué importa:** criterio C3 (hasta 3 parámetros) y vuelve a aparecer el *data clump* de S05 en el borde HTTP. — **Propuesta:** un record `FiltroSolicitudesRequest(estado, tipo, prioridad, legajo)` con `@ParameterObject` (springdoc) y un método `mapper.aFiltro(request)`; el controller queda con un parámetro y sin condicionales.

**O-13 · [Severidad: sugerencia] `web/SolicitudMapper.java:105-111`** — La descripción solo se valida por longitud: diez espacios en blanco cumplen RN-02 ("obligatoria, entre 10 y 500 caracteres"). Es un comportamiento **heredado de la v1** (`estado-inicial`, `SC:54`), no una regresión. — **Por qué importa:** una descripción vacía en la práctica pasa la validación. — **Propuesta:** decidir explícitamente: mantenerlo y declararlo en los límites del trabajo, o aplicar `strip()` y registrarlo como mejora intencional con su test (cambiaría una respuesta del contrato para esa entrada, por lo que requiere la aprobación del autor).

**O-14 · [Severidad: sugerencia] `pom.xml:14-27`** — Quedaron los metadatos vacíos de Spring Initializr: `<name/>`, `<description/>`, `<url/>`, `<license/>`, `<developer/>`, `<scm>` con elementos vacíos. — **Por qué importa:** criterio G3 (documentación); el artefacto se publica sin nombre ni descripción. — **Propuesta:** completar `name` y `description` y eliminar los bloques vacíos.

---

## 3. Seguimiento de las decisiones

| ID | Decisión (aceptada / rechazada) | Justificación si se rechaza | Commit de corrección |
|---|---|---|---|
| O-1 | | | |
| O-2 | | | |
| O-3 | | | |
| O-4 | | | |
| O-5 | | | |
| O-6 | | | |
| O-7 | | | |
| O-8 | | | |
| O-9 | | | |
| O-10 | | | |
| O-11 | | | |
| O-12 | | | |
| O-13 | | | |
| O-14 | | | |

Las observaciones aceptadas se corrigen una por commit con el formato `fix(review): <descripción>`, como indica el plan (Fase 5, punto 5).
