# Pull Request — título y descripción

> Este archivo contiene el texto para copiar en GitHub al abrir el PR `refactor/solicitudes` → `main`.
> El título va en el campo "Title" y todo lo que está debajo de la línea `---` va en la descripción.

## Título

```
Refactoring v2: SOLID, Clean Code y manejo de errores
```

---

## Resumen

Refactoriza el sistema de gestión de solicitudes académicas (v1, tag `estado-inicial`) hacia un diseño en capas con dominio rico (v2), **sin cambiar el contrato HTTP**: mismas rutas, métodos, campos JSON, códigos de estado y reglas de negocio. Resuelve los 12 smells diagnosticados en `docs/tpi/03-deuda-tecnica-code-smells.md` (S01–S12) con 9 refactorings y un commit por técnica, verificados con los 15 tests de caracterización (que **no se modificaron**) y con una batería de 25 llamadas HTTP comparadas antes y después.

## Refactorings

| ID | Técnica (Fowler) | Smells | Principio | Commit |
|---|---|---|---|---|
| R01 | Extract Method | S02, S03 | Clean Code | `9232424` |
| R02 | Replace Type Code with Enum | S04 | Clean Code | `fb7220c` |
| R03 | Move Method | S06, S08 | Cohesión / *Tell, Don't Ask* | `43c190e` |
| R04 | Replace Conditional with Polymorphism | S07 | **OCP** | `bbe0574` |
| R05 | Introduce Parameter Object (A: entradas · B: DTOs de respuesta) | S05 | Clean Code | `f4d44c2`, `28f2ef5` |
| R06 | Extract Class (A) + reorganización de paquetes por capas (B) | S01 | **SRP** | `1c8d5af`, `3b32287` |
| R07 | Replace Error Code with Exception + manejo centralizado | S10 | Clean Code, SRP | `7ab83de` |
| R08 | Introduce Interface + Dependency Injection | S11 | **DIP** | `d3c1ebb` |
| R09 | Rename + Replace Magic Number + Remove Dead Code | S09, S12 | Clean Code | `bd1b115` |

Cada refactoring está documentado con fragmento *antes*, fragmento *después*, pasos seguros aplicados, hash y resultado de los tests en [`docs/tpi/refactorings.md`](refactorings.md).

## Estructura resultante

`dominio/` (entidades, enums con comportamiento y `excepciones/`) · `aplicacion/` (`SolicitudService`, `ReporteService`, `NuevaSolicitud`, `FiltroSolicitudes`, `Notificador`/`NotificadorLog`) · `persistencia/` (repositorios) · `web/` (controllers, `SolicitudMapper`, `ManejadorGlobalErrores`, `dto/`) · `config/` (`ClockConfig`). El paquete `util/` desapareció.

## Métricas antes / después

| Métrica | v1 (`estado-inicial`) | v2 (esta rama) |
|---|---|---|
| Violaciones PMD (ruleset del TRD §4) | **56** | **10** (todas `ShortVariable` sobre `id`, justificadas en R09) |
| Líneas de `SolicitudController` | **433** | **70** |
| Método más largo | 77 líneas (`crear`) | 14 líneas |
| `catch (Exception)` en producción | 6 | 0 |
| Literales de estado escritos a mano | 49 | 0 |
| Cobertura JaCoCo (instrucciones / ramas) | 82 % / 60 % | **86 % / 74 %** |
| Tests de caracterización CP-01..CP-15 | 15/15 en verde | **15/15 en verde, sin modificar** (`git diff estado-inicial -- src/test` vacío) |
| Tests totales | 16 | 16 |
| Tipos de producción | 11 | 36 |
| Líneas de código de producción | 828 | 1154 |

Los dos últimos renglones muestran el costo del diseño en capas: hay más clases y más líneas en total, pero cada una tiene una única responsabilidad y es chica: la más larga es la entidad `Solicitud` (175 líneas, en su mayoría getters y setters), seguida de `SolicitudService` (127 líneas) y de `SolicitudMapper` (112); ningún método supera las 14 líneas. No se afirma que "menos código" sea el objetivo del trabajo.

Evidencias: `docs/evidencias/antes/` y `docs/evidencias/despues/` (capturas de Swagger, de los tests intactos, del error en JSON y del esquema tipado).

## Cambio externo intencional (el único)

El **cuerpo de las respuestas de error** pasa de texto plano a JSON. El código HTTP y el texto del mensaje no cambian.

| | v1 | v2 |
|---|---|---|
| `Content-Type` | `text/plain;charset=UTF-8` | `application/json;charset=UTF-8` |
| Cuerpo (409) | `Error: Transición inválida: CANCELADA -> EN_ANALISIS` | `{"status":409,"error":"Conflict","mensaje":"Transición inválida: CANCELADA -> EN_ANALISIS","timestamp":"2026-11-04T10:29:24"}` |

Los tests de caracterización verifican el código HTTP y que el cuerpo **contenga** el mensaje, por lo que pasan en ambas versiones. Los errores de framework (JSON malformado, id no numérico, ruta inexistente) conservan su código 400/404 y usan el mismo formato; los errores inesperados responden 500 con un mensaje genérico, sin detalles internos. Detalle y capturas en la ficha de R07.

## Cómo se verificó

- `.\mvnw.cmd test` → `Tests run: 16, Failures: 0, Errors: 0` después de **cada** refactoring.
- Batería de 25 llamadas HTTP con la app levantada (éxitos, 400, 404, 409 y errores de framework), capturada antes y después de los cambios que tocaban la capa web, con `diff`: respuestas de éxito idénticas byte a byte; los errores conservan su código y solo cambia la forma del cuerpo. Esta verificación es necesaria porque los tests de caracterización son transaccionales y no detectan, por ejemplo, un `LazyInitializationException` (que apareció durante R06 y se corrigió con `@EntityGraph`).
- `.\mvnw.cmd pmd:pmd` y `.\mvnw.cmd test jacoco:report` para las métricas de arriba.

## Code review

La revisión con checklist está en [`docs/tpi/07-code-review-checklist.md`](07-code-review-checklist.md): 14 observaciones (0 bloqueantes, 3 mayores, 8 menores, 3 sugerencias), producidas con asistencia de IA y sujetas a la decisión del autor de aceptarlas o rechazarlas. Las aceptadas se corrigen en commits `fix(review): ...` dentro de este mismo PR.

## Para quien revisa

1. Leer [`docs/tpi/refactorings.md`](refactorings.md) (una ficha por refactoring).
2. Revisar commit por commit en orden (`git log --reverse main..refactor/solicitudes`): cada uno es independiente y deja los tests en verde.
3. Verificar que `src/test/java/ar/edu/utn/tup/solicitudes/api/` no tiene cambios.
4. Aplicar el checklist de [`docs/tpi/07-code-review-checklist.md`](07-code-review-checklist.md).

**Merge:** merge commit (no squash), para conservar los commits por refactoring.
