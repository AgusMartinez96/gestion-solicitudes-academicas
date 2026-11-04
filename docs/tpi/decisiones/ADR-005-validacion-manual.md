# ADR-005 — Validación de entrada manual y centralizada (sin Bean Validation)

- **Estado:** aceptada
- **Origen:** observación **O-3** del code review (`docs/tpi/07-code-review-checklist.md`)
- **Fecha:** 2026-11-04

## Contexto

El TRD original (§1) preveía migrar la validación manual de la v1 a **Jakarta Bean Validation** (`spring-boot-starter-validation`) en la v2. Durante el refactoring la validación se movió a un único punto (`NuevaSolicitud.desde(...)` para el registro y los métodos `parsear` de los enums `EstadoSolicitud`, `TipoSolicitud` y `Prioridad`), pero nunca se usó `@Valid`, `@Size` ni `@NotBlank`: la dependencia quedó declarada sin ninguna referencia en `src/main`, y el TRD afirmaba algo que el código no hacía.

El contrato HTTP no puede cambiar entre v1 y v2 (CLAUDE.md §3.2). Ese contrato incluye:

- el **orden** de las validaciones: descripción → tipo → prioridad → legajo (404);
- los **mensajes exactos** (por ejemplo `La descripción debe tener entre 10 y 500 caracteres`), que verifican los tests de caracterización.

## Decisión

Mantener la **validación manual** en la v2, centralizada en el dominio y la capa de aplicación, y **quitar** `spring-boot-starter-validation` y `spring-boot-starter-validation-test` del `pom.xml`. Se actualizó el TRD en consecuencia.

## Alternativas consideradas

1. **Adoptar Bean Validation** (`@Size(min = 10, max = 500)` sobre `RegistrarSolicitudRequest`). Rechazada:
   - `@Valid` valida todos los campos y acumula las violaciones; garantizar el orden descripción → tipo → prioridad → legajo y un único mensaje por respuesta exigiría código adicional que anula la ventaja declarativa.
   - La excepción `MethodArgumentNotValidException` tiene su propio formato de mensaje; habría que traducirla al texto exacto del contrato en el `ManejadorGlobalErrores`.
   - Tipo y prioridad llegan como `String` y se validan contra enums con mensajes propios, algo que Bean Validation no resuelve sin validadores personalizados.
   - Habría un riesgo concreto de cambiar una respuesta del contrato, que es lo que el TPI exige preservar.
2. **Dejar la dependencia declarada sin usarla.** Rechazada: es código muerto (checklist C5) y la documentación afirmaría algo falso.

## Consecuencias

- (+) El contrato HTTP (orden y mensajes) queda intacto; los 15 tests de caracterización siguen en verde sin cambios.
- (+) Una dependencia menos y documentación coherente con el código.
- (+) La validación vive en un solo lugar, ejercitable con tests unitarios sin levantar Spring.
- (−) Las reglas de formato no se declaran sobre el DTO ni aparecen automáticamente en el esquema OpenAPI.
- (−) Si en el futuro se admite acumular errores o cambiar el formato de los mensajes, convendría reevaluar Bean Validation. Eso sería un cambio de contrato y requeriría una decisión explícita.
