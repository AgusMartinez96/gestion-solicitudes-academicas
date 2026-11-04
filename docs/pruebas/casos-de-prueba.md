# Casos de prueba de caracterización (CP-01 … CP-15)

Los casos se ejecutan por HTTP (MockMvc) contra la API, sin conocer ninguna clase interna. Por eso sirven igual para la v1 y para la v2.

- Clase: `src/test/java/ar/edu/utn/tup/solicitudes/api/SolicitudesApiCaracterizacionTest.java`
- Cada test corre en una transacción con rollback, así los datos semilla quedan iguales para todos.
- Los mensajes de error se verifican por contenido, por lo que valen para el cuerpo en texto plano (v1) y para el JSON (v2).

| ID | Requerimiento | Acción | Resultado esperado | Resultado v1 | Resultado v2 |
|---|---|---|---|---|---|
| CP-01 | RF-01, RN-05, RN-06 | POST `CAMBIO_COMISION`, prioridad `MEDIA`, legajo `L-1001` | 201; estado `PENDIENTE`; prioridad `MEDIA`; vencimiento − creación = 3 días | ✅ | ✅ |
| CP-02 | RN-04, RN-06 | POST `REVISION_EXAMEN` sin prioridad, `L-1002` | 201; prioridad `ALTA`; vencimiento − creación = 2 días (5 / 2) | ✅ | ✅ |
| CP-03 | RN-02 | POST con descripción `"corta"` | 400; contiene `La descripción debe tener entre 10 y 500 caracteres` | ✅ | ✅ |
| CP-04 | RN-03 | POST con tipo `"BECA"` | 400; contiene `Tipo de solicitud inválido: BECA` | ✅ | ✅ |
| CP-05 | RN-01 | POST con legajo `"L-9999"` | 404; contiene `Estudiante no encontrado: L-9999` | ✅ | ✅ |
| CP-06 | RF-02 | GET `/api/solicitudes/2` | 200; estado `EN_ANALISIS`; responsable.nombre = Carla Gómez; fechas sin decimales | ✅ | ✅ |
| CP-07 | RF-02 | GET `/api/solicitudes/99` | 404; contiene `Solicitud no encontrada: 99` | ✅ | ✅ |
| CP-08 | RF-03 | GET `?estado=PENDIENTE` → ids [1, 5]; `?estado=PENDIENTE&legajo=L-1003` → ids [5]; `?estado=FOO` → 400 | según lo indicado | ✅ | ✅ |
| CP-09 | RF-04, RN-07, RN-12 | PATCH estado de la solicitud 5 a `EN_ANALISIS` | 200; estado `EN_ANALISIS`; `fechaActualizacion` no nula | ✅ | ✅ |
| CP-10 | RN-07 | PATCH estado de la 4 (`CANCELADA`) a `EN_ANALISIS`; PATCH estado de la 1 a `"FOO"` | 409 `Transición inválida: CANCELADA -> EN_ANALISIS`; 400 `Estado inválido: FOO` | ✅ | ✅ |
| CP-11 | RN-08 | PATCH la 1 a `EN_ANALISIS` (200) y después a `RESUELTA` | 409; contiene `requiere un responsable asignado` | ✅ | ✅ |
| CP-12 | RN-07, RN-12 | PATCH estado de la 2 a `RESUELTA` | 200; estado `RESUELTA` | ✅ | ✅ |
| CP-13 | RF-05, RN-10 | PATCH responsable 1 en la solicitud 5 | 200; estado `EN_ANALISIS`; responsable.id = 1 | ✅ | ✅ |
| CP-14 | RN-09 | PATCH responsable 3 (inactivo) en la 5; responsable 1 en la 3 (cerrada); responsable 99 en la 5 | 409 / 409 / 404 con los mensajes del contrato | ✅ | ✅ |
| CP-15 | RF-06, RN-11 | GET `/api/reportes/resumen` | 200; total 5; `PENDIENTE` 2; `EN_ANALISIS` 1; `RESUELTA` 1; `RECHAZADA` 0; `CANCELADA` 1; cada tipo 1; vencidas 1 | ✅ | ✅ |

## Resultado de la ejecución sobre la v1

`.\mvnw.cmd test` → `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0` (15 de caracterización + `contextLoads`).

Evidencias en `docs/evidencias/antes/` (`surefire/`, `jacoco/`, `pmd/`, `capturas/`).

## Resultado de la ejecución sobre la v2

`.\mvnw.cmd test` en `main` → `Tests run: 105, Failures: 0, Errors: 0, Skipped: 0`: los 16 originales (15 de caracterización + `contextLoads`, **sin modificar**) más 89 nuevos (63 unitarios de dominio, 11 del service, 4 del manejador de errores y 11 de integración no transaccional).

El mismo archivo `SolicitudesApiCaracterizacionTest` pasa 15/15 en `estado-inicial` y en `main`. El detalle de la estrategia, las métricas antes/después y la comprobación del `@EntityGraph` están en [`docs/tpi/06-pruebas-verificacion.md`](../tpi/06-pruebas-verificacion.md). Evidencias en `docs/evidencias/despues/` (`surefire/`, `jacoco/`, `pmd/`, `capturas/`).
