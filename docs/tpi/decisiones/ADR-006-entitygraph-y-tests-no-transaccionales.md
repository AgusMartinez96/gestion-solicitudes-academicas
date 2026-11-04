# ADR-006 — `@EntityGraph` explícito y tests de integración no transaccionales

- **Estado:** aceptada
- **Origen:** hallazgo de R06, observaciones O-7 y O-11 del code review, comprobación de la Fase 6
- **Fecha:** 2026-11-04

## Contexto

En la v2 la transacción dejó de estar en el controller (acoplamiento de la v1, S11) y pasó al service. El mapeo a DTO se hace en la capa web, **después** de que el service terminó. Con `spring.jpa.open-in-view=false` la sesión de JPA se cierra al salir del service, y `Solicitud.responsable` es `LAZY`: leer `solicitud.getResponsable().getNombre()` fuera de la transacción lanza `LazyInitializationException`.

Los 16 tests de entonces pasaban igual, porque los tests de caracterización son `@Transactional`: MockMvc corre dentro de una transacción de test que mantiene la sesión abierta. El error apareció recién al levantar la aplicación y llamar por HTTP (hallazgo de R06).

## Decisión

1. **Cargar lo que necesita la respuesta dentro del service**, con `@EntityGraph(attributePaths = { "estudiante", "responsable" })` en métodos del repositorio con nombre explícito: `findDetalleById` y `findAllConDetalle` (O-7). El reporte usa `findAll()` simple, que no necesita esas relaciones. Un comentario en el repositorio explica el porqué.
2. **Mantener `open-in-view=false`** y no volver a abrir la sesión en la capa web.
3. **Agregar una prueba de integración no transaccional** (`IntegracionHttpNoTransaccionalTest`): servidor real en puerto aleatorio, cliente HTTP real (`java.net.http.HttpClient`), base H2 propia (`jdbc:h2:mem:integracion`) y `@DirtiesContext`. Es la red de seguridad automática para este riesgo (O-11).

## Alternativas consideradas

1. **`spring.jpa.open-in-view=true`.** Rechazada: oculta el problema y mantiene la sesión abierta durante toda la petición, incluida la serialización; es un antipatrón conocido.
2. **Cambiar `responsable` a `EAGER`.** Rechazada: trae el responsable en consultas que no lo necesitan y cambia el modelo por un problema de la capa web.
3. **Mapear a DTO dentro de la transacción del service.** Rechazada: llevaría tipos de la capa web a la capa de aplicación.
4. **Confiar solo en los tests de caracterización.** Rechazada: se demostró que no detectan el defecto.

## Comprobación (Fase 6)

Se quitó temporalmente el `@EntityGraph` (sin commitear) y se corrieron ambas suites:

| Suite | Con `@EntityGraph` | Sin `@EntityGraph` |
|---|---|---|
| Caracterización (MockMvc, transaccional) | 15/15 | **15/15** (no detecta el defecto) |
| `IntegracionHttpNoTransaccionalTest` | 11/11 | **7/11**: 4 fallan, con `LazyInitializationException: Could not initialize proxy [...Responsable#1] - no session` |

Después se restauró el archivo con `git checkout`. Detalle en `docs/tpi/06-pruebas-verificacion.md` §1.

## Consecuencias

- (+) El defecto queda cubierto por un test automático que falla si alguien quita el grafo.
- (+) Cada consulta carga solo lo que necesita.
- (−) Hay que recordar que cualquier relación `LAZY` leída por el mapper debe cargarse en el service; el test de integración es la guía.
- (−) El test arranca un contexto completo (≈5 s), más lento que los unitarios.
