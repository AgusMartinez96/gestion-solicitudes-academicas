# ADR-002 — Filtrado en memoria con `FiltroSolicitudes.coincideCon()`

- **Estado:** aceptada (con mejora futura declarada)
- **Refactorings relacionados:** R05 (Introduce Parameter Object)
- **Fecha:** 2026-11-04

## Contexto

La búsqueda `GET /api/solicitudes?estado=&tipo=&prioridad=&legajo=` combina hasta cuatro filtros opcionales. En la v1 se hacía `findAll()` y un `for` con cuatro `if` anidados y variables `lista`, `lista2`. En la v2 hay que decidir si se mantiene el filtrado en memoria o se pasa a consultas en base de datos.

## Decisión

Se mantiene el **filtrado en memoria**, pero encapsulado en un objeto: `FiltroSolicitudes` es un `record` (Parameter Object) con el método `coincideCon(Solicitud)`, donde `null` significa "sin filtro". `SolicitudService.buscar` trae las solicitudes con `findAllConDetalle()`, conserva las que coinciden y las ordena por id.

## Alternativas consideradas

1. **Specifications de Spring Data JPA** o métodos derivados del repositorio. Rechazada por ahora: con un volumen de datos pequeño (H2 en memoria, datos sintéticos) la ganancia es nula y agregaría complejidad que el TPI no evalúa. Además cambiar el mecanismo de consulta podría alterar el orden o el comportamiento del contrato.
2. **Consulta JPQL con parámetros opcionales.** Rechazada por el mismo motivo.

## Consecuencias

- (+) Los cuatro `if` anidados desaparecen: la regla de coincidencia está en un solo lugar, es una expresión corta y se prueba sin base de datos.
- (+) El contrato HTTP no cambia (mismo orden por id, mismos filtros).
- (−) Con muchos registros el costo crece linealmente, porque se leen todas las solicitudes (con sus estudiantes y responsables) para devolver pocas.
- **Mejora futura:** pasar el filtro a la base con `Specification` o JPQL y paginar el resultado cuando el volumen lo justifique. El borde HTTP (los 4 parámetros) no cambiaría.
