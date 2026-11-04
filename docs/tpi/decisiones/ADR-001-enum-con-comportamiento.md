# ADR-001 — Enums con comportamiento en lugar de jerarquía de clases o Strategy

- **Estado:** aceptada
- **Refactorings relacionados:** R02 (Replace Type Code with Enum), R03 y R04 (comportamiento dentro del enum)
- **Fecha:** 2026-11-04

## Contexto

En la v1, el tipo, la prioridad y el estado de una solicitud eran `String`. Las reglas que dependían del tipo (plazo en días y prioridad por defecto) estaban en dos `switch`/`if` paralelos sobre el mismo texto, en `Utils.calcularDias` y en el controller, y las reglas de transición de estados estaban en un método privado del controller. Agregar un tipo nuevo (`BECA`, por ejemplo) obligaba a tocar 6 lugares en 2 archivos, y 2 de ellos fallaban en silencio (`docs/tpi/02-calidad-externa-interna.md` §3).

Había que decidir dónde vive ese conocimiento: ¿en una jerarquía de clases, en una estrategia por tipo, o en los propios enums?

## Decisión

`TipoSolicitud`, `Prioridad` y `EstadoSolicitud` son **enums de Java con comportamiento**:

- `TipoSolicitud` conoce su `plazoDias()` y su `prioridadPorDefecto()`.
- `Prioridad` sabe ajustar el plazo (`ajustarPlazo`: la prioridad `ALTA` lo divide por 2 con un mínimo de 1 día).
- `EstadoSolicitud` conoce la máquina de estados (`puedeTransicionarA`, `esCerrado`, `esResolucion`, `requiereResponsable`).
- Cada enum tiene `parsear(String)` que valida el texto de entrada y lanza `DatoInvalidoException`.

Se persisten con `@Enumerated(EnumType.STRING)` más `@JdbcTypeCode(SqlTypes.VARCHAR)`, para que la base y el JSON no cambien.

## Alternativas consideradas

1. **Jerarquía de clases o patrón Strategy** (una clase por tipo con su política). Rechazada: para 5 tipos con dos datos cada uno (plazo y prioridad) sería más código y más archivos sin ganancia real; el compilador ya fuerza el manejo de cada constante en los `switch` sobre enums.
2. **Tabla de configuración en base de datos.** Rechazada: agrega persistencia y administración que el alcance no pide.
3. **Dejar los `String` y centralizar en constantes.** Rechazada: no elimina los errores de tipeo ni da tipado fuerte.

## Consecuencias

- (+) Agregar un tipo es **agregar una constante** con su plazo y su prioridad por defecto; no se tocan el service ni el controller (OCP). Los `switch` exhaustivos del compilador avisan si falta cubrir una constante.
- (+) Las reglas se prueban sin Spring (`EstadoSolicitudTest`, `TipoSolicitudTest`, `PrioridadTest`).
- (−) Los valores viven en el código: cambiar un plazo exige recompilar y desplegar.
- (−) Si los plazos pasaran a ser configurables por la facultad, convendría migrar a una estrategia o a datos; esta decisión se puede revisar entonces.
- Hallazgo de R02: con Hibernate 7 sobre H2, `@Enumerated(EnumType.STRING)` solo genera columnas `enum` nativas; hizo falta `@JdbcTypeCode(SqlTypes.VARCHAR)` para conservar el esquema `VARCHAR` del contrato.
