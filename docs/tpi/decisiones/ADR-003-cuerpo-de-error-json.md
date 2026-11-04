# ADR-003 — Cuerpo de error en JSON (`ErrorResponse`): el único cambio externo intencional

- **Estado:** aceptada
- **Refactorings relacionados:** R07 (Replace Error Code with Exception + manejo centralizado); ajuste de mensajes en O-9 del code review
- **Fecha:** 2026-11-04

## Contexto

La regla del trabajo es que el contrato HTTP sea **idéntico** en v1 y v2. En la v1, los errores se lanzaban como `RuntimeException("404|...")`, cada endpoint tenía un `catch (Exception)` y un método `procesar` que leía el prefijo del mensaje para elegir el código HTTP. La respuesta de error era **texto plano** (`Content-Type: text/plain;charset=UTF-8`, cuerpo `Error: <mensaje>`), mientras que las respuestas de éxito eran JSON.

Al reemplazar ese mecanismo por excepciones propias y un único `@RestControllerAdvice`, hay que decidir qué forma tiene el cuerpo del error.

## Decisión

El manejador `ManejadorGlobalErrores` responde con un `ErrorResponse` en JSON:

```json
{"status":409,"error":"Conflict","mensaje":"Transición inválida: CANCELADA -> EN_ANALISIS","timestamp":"2026-11-04T10:29:24"}
```

- El **código HTTP** y el **texto del mensaje** no cambian respecto de la v1.
- `DatoInvalidoException` → 400, `RecursoNoEncontradoException` → 404, `ReglaDeNegocioException` → 409.
- Los errores inesperados responden 500 con el mensaje genérico `Error interno del servidor`, sin detalles internos (se registran en el log).
- Es el **único cambio externo permitido**, documentado como mejora intencional (`docs/tpi/refactorings.md`, R07, y `docs/tpi/06-pruebas-verificacion.md` §6).

## Alternativas consideradas

1. **Mantener el texto plano `Error: ...`.** Rechazada: obligaba a conservar un manejador artesanal que escribe el cuerpo a mano, y el JSON tipado es lo habitual en una API REST y se documenta en OpenAPI (`ErrorResponse`).
2. **Usar `ProblemDetail` (RFC 9457) de Spring.** Rechazada: agrega campos y un formato distinto al contrato (`status`, `error`, `mensaje`, `timestamp`) que se quiere conservar simple.

## Consecuencias

- (+) Un solo lugar traduce excepciones a HTTP; desaparecen `catch (Exception)` y los prefijos `"404|"`.
- (+) Los tests de caracterización verifican el **código HTTP** y que el cuerpo **contenga** el mensaje, así que pasan 15/15 sobre ambas versiones sin modificarse.
- (−) Un cliente que leyera el cuerpo como texto plano debería adaptarse: es un cambio externo real, aunque declarado.
- Ajuste posterior (O-9): los tres errores que genera el framework ahora tienen mensaje en español (`Parámetro inválido: id`, `El cuerpo de la solicitud no es válido`, `Recurso no encontrado`); el código HTTP se mantiene.
