# 06 — Pruebas y verificación

Este documento muestra, con números reproducibles, que la v2 (`estado-refactorizado`, hoy `main`) **se comporta igual que la v1** (`estado-inicial`) desde afuera, y qué red de seguridad se agregó para que el diseño interno nuevo no se rompa.

## 1. Estrategia

Hay tres capas de pruebas, cada una con un propósito distinto:

| Capa | Dónde | Qué prueba | Por qué hace falta |
|---|---|---|---|
| **Caracterización (caja negra)** | `src/test/.../api/SolicitudesApiCaracterizacionTest` (CP-01..CP-15) | El contrato HTTP: rutas, JSON, códigos y mensajes, vía MockMvc. | Es la **red de seguridad del refactoring**. Se escribió sobre la v1 y no se modificó durante el refactoring (`git diff estado-inicial -- src/test/java/ar/edu/utn/tup/solicitudes/api` vacío), así que prueba que el comportamiento externo no cambió. |
| **Unitarias (sin Spring)** | `dominio/` (4 clases), `aplicacion/SolicitudServiceTest` y `web/ManejadorGlobalErroresTest` | Reglas del dominio (transiciones, plazos, RN-08..RN-12), el service con reloj fijo y notificador falso, y el manejador de errores. | Antes del refactoring las reglas estaban mezcladas con HTTP y no se podían probar solas. Que ahora se puedan es el **beneficio medible de DIP y de mover las reglas al dominio** (R03, R04, R08). Corren en milisegundos. |
| **Integración no transaccional** | `integracion/IntegracionHttpNoTransaccionalTest` | El flujo completo con servidor real (puerto aleatorio) y un cliente HTTP real (`java.net.http.HttpClient`), sobre su propia base H2 en memoria. | Cubre un hueco de la caracterización, descrito abajo. |

### Por qué hizo falta la prueba no transaccional (hallazgo de R06)

Los tests de caracterización son `@Transactional`: toda la petición MockMvc corre dentro de una transacción de test que **mantiene abierta la sesión de JPA**. Durante R06 se movió el mapeo a DTOs a la capa web, fuera del service; con `spring.jpa.open-in-view=false`, al terminar el service la sesión se cierra y acceder a `solicitud.getResponsable().getNombre()` (relación `LAZY`) lanza `LazyInitializationException`. Ese error **apareció recién al levantar la app y llamar por HTTP**: los 16 tests seguían en verde. Se corrigió con `@EntityGraph`, y el code review (O-7) lo dejó en métodos explícitos (`findDetalleById`, `findAllConDetalle`).

El test de integración no transaccional es la red de seguridad automática para ese riesgo (observación **O-11** del code review, diferida a esta fase).

#### Comprobación de que el test detecta el problema

Se quitó temporalmente el `@EntityGraph` de `SolicitudRepository` (sin commitear) y se corrieron ambas suites:

| Suite | Con `@EntityGraph` | Sin `@EntityGraph` |
|---|---|---|
| Caracterización (MockMvc, transaccional) | 15/15 ✅ | **15/15 ✅** (no detecta el problema) |
| `IntegracionHttpNoTransaccionalTest` | 11/11 ✅ | **7/11 ✅, 4 ❌** (3 fallos y 1 error; entre ellos `listar`, `obtenerConEstudianteYResponsable` y `resolverConResponsable`), con `LazyInitializationException: Could not initialize proxy [...Responsable#1] - no session` |

Después se restauró el archivo con `git checkout -- SolicitudRepository.java` y se verificó que el árbol quedó limpio. Esto demuestra que la prueba nueva **detecta el defecto que la de caracterización no puede ver**.

### Aislamiento de datos

La prueba de integración usa `spring.datasource.url=jdbc:h2:mem:integracion` (base propia) y `@DirtiesContext`, así que no comparte datos ni esquema con los otros tests (que usan la base `solicitudes` dentro de transacciones con rollback). Sus aserciones no dependen del orden: los tests que modifican datos crean su propia solicitud y el reporte se verifica por coherencia (el total coincide con la suma de `porEstado` y de `porTipo`), no por cifras fijas.

## 2. Resumen de tests

| Tipo | Clase | Tests |
|---|---|---|
| Caracterización (HTTP, MockMvc) | `api.SolicitudesApiCaracterizacionTest` | 15 |
| Contexto | `SolicitudesApplicationTests` | 1 |
| Unitario de dominio | `EstadoSolicitudTest` (36), `SolicitudTest` (11), `PrioridadTest` (10), `TipoSolicitudTest` (6) | 63 |
| Unitario de aplicación | `aplicacion.SolicitudServiceTest` | 11 |
| Unitario de web | `web.ManejadorGlobalErroresTest` | 4 |
| Integración no transaccional | `integracion.IntegracionHttpNoTransaccionalTest` | 11 |
| **Total** | | **105** (v1: 16) |

Las cifras de `EstadoSolicitudTest`, `PrioridadTest` y `TipoSolicitudTest` cuentan cada caso de los `@ParameterizedTest` (la tabla completa de 25 transiciones, los límites de `ajustarPlazo` —incluido el mínimo de 1 día— y los 5 tipos).

## 3. Casos CP-01..CP-15 en v1 y v2

El **mismo archivo de tests** se ejecutó sobre ambas versiones.

| ID | Qué verifica | Resultado v1 (`estado-inicial`) | Resultado v2 (`main`) |
|---|---|---|---|
| CP-01 | Alta de `CAMBIO_COMISION` con prioridad `MEDIA`: 201, `PENDIENTE`, vence a 3 días | ✅ | ✅ |
| CP-02 | Alta de `REVISION_EXAMEN` sin prioridad: `ALTA` y plazo 5/2 = 2 días | ✅ | ✅ |
| CP-03 | Descripción corta: 400 con el mensaje de longitud | ✅ | ✅ |
| CP-04 | Tipo inexistente: 400 `Tipo de solicitud inválido: BECA` | ✅ | ✅ |
| CP-05 | Legajo inexistente: 404 | ✅ | ✅ |
| CP-06 | Consulta por id con responsable y fechas sin decimales | ✅ | ✅ |
| CP-07 | Consulta de solicitud inexistente: 404 | ✅ | ✅ |
| CP-08 | Filtros por estado y legajo; estado inválido: 400 | ✅ | ✅ |
| CP-09 | `PENDIENTE` → `EN_ANALISIS` con `fechaActualizacion` | ✅ | ✅ |
| CP-10 | Transición inválida: 409; estado inválido: 400 | ✅ | ✅ |
| CP-11 | Resolver sin responsable: 409 | ✅ | ✅ |
| CP-12 | Resolver con responsable: 200 | ✅ | ✅ |
| CP-13 | Asignar responsable pasa a `EN_ANALISIS` (RN-10) | ✅ | ✅ |
| CP-14 | Responsable inactivo (409), solicitud cerrada (409), responsable inexistente (404) | ✅ | ✅ |
| CP-15 | Reporte: totales, claves de estado y tipo, vencidas | ✅ | ✅ |

Resultado v1: `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`. Resultado v2: `Tests run: 105, Failures: 0, Errors: 0, Skipped: 0` (los 16 originales más 89 nuevos).

## 4. Cómo reproducirlo

Desde la terminal de VS Code (PowerShell), en la raíz del proyecto:

```powershell
# v1
git checkout estado-inicial
.\mvnw.cmd test            # Tests run: 16, Failures: 0

# v2
git checkout main
.\mvnw.cmd test            # Tests run: 105, Failures: 0

# Los tests de caracterización no cambiaron (debe no mostrar diferencias)
git diff estado-inicial -- src/test/java/ar/edu/utn/tup/solicitudes/api

# Cobertura y análisis estático de la v2
.\mvnw.cmd test jacoco:report   # target\site\jacoco\index.html
.\mvnw.cmd pmd:pmd              # target\reports\pmd.html
```

Para ver la comprobación del `@EntityGraph`: borrar las dos líneas `@EntityGraph(...)` de `persistencia/SolicitudRepository.java` y correr `.\mvnw.cmd test -Dtest=IntegracionHttpNoTransaccionalTest` (falla); después `git checkout -- src/main/java/ar/edu/utn/tup/solicitudes/persistencia/SolicitudRepository.java`.

Evidencias de ejecución: `docs/evidencias/antes/` y `docs/evidencias/despues/` (`surefire/`, `jacoco/`, `pmd/`, `capturas/`).

## 5. Métricas antes / después

Todas medidas sobre el código y los reportes del repositorio.

| Métrica | v1 (`estado-inicial`) | v2 (`main`) |
|---|---|---|
| Violaciones PMD (total) | **56** | **9** |
| ↳ `ShortVariable` | 28 | 9 (todas `id`: nombre exigido por JPA y por el contrato JSON) |
| ↳ `CyclomaticComplexity` | 6 | 0 |
| ↳ `AvoidCatchingGenericException` | 6 | 0 |
| ↳ `NcssCount` | 5 | 0 |
| ↳ `CognitiveComplexity` | 3 | 0 |
| ↳ `NPathComplexity` | 3 | 0 |
| ↳ `SystemPrintln` | 3 | 0 |
| ↳ `AvoidLiteralsInIfCondition` | 1 | 0 |
| ↳ `ExcessiveParameterList` | 1 | 0 |
| Líneas de `SolicitudController` | **433** | **100** (≈70 de lógica y 30 de anotaciones `@ApiResponse` que documentan los errores en OpenAPI, O-10) |
| Método más largo | 77 líneas (`crear`) | 15 líneas (firma y llaves incluidas) |
| Tipos de producción (archivos `.java` en `src/main`) | 11 | 39 |
| Clases analizadas por JaCoCo | 8 | 35 |
| Cantidad de tests | 16 | **105** |
| Cobertura de instrucciones (JaCoCo) | 82,2 % | **95,5 %** |
| Cobertura de ramas (JaCoCo) | 60,5 % | **86,1 %** |
| Cobertura de líneas (JaCoCo) | 81,1 % | **93,9 %** |

Notas de lectura:

- Las violaciones `ShortVariable` de la v1 eran variables de nombre corto; las 9 restantes son el campo `id` y sus accesores. Se dejaron a propósito (R09).
- Con solo los tests de caracterización la v2 tiene 86,1 % de instrucciones y 74,8 % de ramas (medido con `clean`); los tests nuevos la suben a 95,5 % y 86,1 %. Las cifras se midieron siempre con `.\mvnw.cmd clean test jacoco:report`, porque el agente de JaCoCo acumula datos entre ejecuciones si no se limpia `target\`. La cobertura no demuestra por sí sola la calidad: muestra que las reglas ahora se ejercitan directamente.
- El crecimiento de tipos (11 → 39) es el costo del diseño en capas, ya discutido en el PR.

## 6. Cambios externos intencionales

El contrato HTTP (rutas, métodos, campos JSON, códigos y reglas de negocio) es idéntico en v1 y v2. Las **únicas diferencias externas** están en el **cuerpo de las respuestas de error**:

### 6.1 Formato del cuerpo de error (R07)

| | v1 | v2 |
|---|---|---|
| `Content-Type` | `text/plain;charset=UTF-8` | `application/json;charset=UTF-8` |
| 409 (transición inválida) | `Error: Transición inválida: CANCELADA -> EN_ANALISIS` | `{"status":409,"error":"Conflict","mensaje":"Transición inválida: CANCELADA -> EN_ANALISIS","timestamp":"2026-11-04T10:29:24"}` |

El código HTTP y el texto del mensaje no cambian. Los tests de caracterización verifican el código y que el cuerpo **contenga** el mensaje, por eso pasan en ambas versiones.

### 6.2 Mensajes de errores de framework en español (O-9)

Solo afecta a los tres errores que genera el framework y no la aplicación; el código HTTP se mantiene:

| Caso | Código | Antes de O-9 (fin de R07) | Ahora |
|---|---|---|---|
| `GET /api/solicitudes/abc` | 400 | `Failed to convert 'id' with value: 'abc'` | `Parámetro inválido: id` |
| `POST` con JSON malformado | 400 | `Failed to read request` | `El cuerpo de la solicitud no es válido` |
| Ruta inexistente | 404 | `Not Found` | `Recurso no encontrado` |

Los errores inesperados responden `500` con el mensaje genérico `Error interno del servidor`, sin detalles internos (verificado por `ManejadorGlobalErroresTest`).

### 6.3 Qué no cambió

Se verificó con una batería de 35 llamadas HTTP contra la app levantada, antes y después de cada corrección del code review: las respuestas de éxito son idénticas byte a byte y los errores conservan su código, salvo el texto de los tres mensajes anteriores. La prueba de integración automatiza ahora esa verificación.
