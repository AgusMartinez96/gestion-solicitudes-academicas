# 01 — PRD: Sistema de Gestión de Solicitudes Académicas

## 1. Contexto y problema

En una facultad, los estudiantes hacen trámites (certificados, revisiones de examen, equivalencias, cambios de comisión) por mail o en ventanilla. No hay trazabilidad: no se sabe en qué estado está cada pedido, quién lo atiende ni cuáles están vencidos.

El sistema centraliza esas **solicitudes académicas**: permite registrarlas, clasificarlas, asignarlas a un responsable, cambiar su estado, consultarlas y obtener un reporte simple.

> Para el TPI, el foco **no** es la cantidad de funcionalidad, sino la mejora de calidad interna entre v1 y v2. El alcance funcional es chico a propósito.

## 2. Actores

| Actor | Descripción | Interacción |
|---|---|---|
| **Estudiante** | Persona que inicia el trámite. Se identifica por **legajo**. | Registra solicitudes y consulta las suyas. |
| **Responsable** | Personal administrativo o docente de un **área** (Bedelía, Departamento Alumnos, Secretaría Académica). | Toma solicitudes, cambia su estado. |
| **Coordinación** | Rol de supervisión. | Asigna responsables y consulta el reporte. |

> No hay autenticación (fuera de alcance). Los actores interactúan con la API por Swagger UI o curl.

## 3. Alcance

### Dentro del alcance
- RF-01 a RF-07 (abajo).
- Persistencia en base H2 en memoria, con datos sintéticos precargados.
- Documentación de la API con Swagger UI.

### Fuera del alcance
- Login, roles y seguridad.
- Frontend propio (se usa Swagger UI para la demo).
- Notificaciones reales por mail (en v1 se "simula" con un log).
- Adjuntos de archivos.

## 4. Requerimientos funcionales

| ID | Requerimiento | Operación verificable |
|---|---|---|
| **RF-01** | Registrar una solicitud indicando legajo del estudiante, tipo, descripción y, opcionalmente, prioridad. | `POST /api/solicitudes` |
| **RF-02** | Consultar una solicitud por id. | `GET /api/solicitudes/{id}` |
| **RF-03** | Buscar/filtrar solicitudes por estado, tipo, prioridad y/o legajo (filtros combinables y opcionales). | `GET /api/solicitudes?estado=&tipo=&prioridad=&legajo=` |
| **RF-04** | Cambiar el estado de una solicitud respetando las transiciones válidas. | `PATCH /api/solicitudes/{id}/estado` |
| **RF-05** | Asignar un responsable a una solicitud. | `PATCH /api/solicitudes/{id}/responsable` |
| **RF-06** | Obtener un reporte resumen: cantidad por estado, cantidad por tipo y cantidad de vencidas. | `GET /api/reportes/resumen` |
| **RF-07** | Listar estudiantes y responsables (datos de soporte). | `GET /api/estudiantes`, `GET /api/responsables` |

## 5. Reglas de negocio

| ID | Regla |
|---|---|
| **RN-01** | El legajo debe corresponder a un estudiante existente. Si no existe → error 404. |
| **RN-02** | La descripción es obligatoria y debe tener entre 10 y 500 caracteres. Si no → 400. |
| **RN-03** | El tipo es obligatorio y debe ser uno de: `CERTIFICADO_ALUMNO_REGULAR`, `REVISION_EXAMEN`, `EQUIVALENCIA`, `CAMBIO_COMISION`, `OTRO`. Si no → 400. |
| **RN-04** | Si no se informa prioridad, se asigna por defecto según el tipo: `REVISION_EXAMEN` → `ALTA`; `CAMBIO_COMISION` y `CERTIFICADO_ALUMNO_REGULAR` → `MEDIA`; `EQUIVALENCIA` y `OTRO` → `BAJA`. Si se informa, debe ser `BAJA`, `MEDIA` o `ALTA`; si no → 400. |
| **RN-05** | Toda solicitud nueva nace en estado `PENDIENTE`, con fecha de creación = ahora. |
| **RN-06** | La fecha de vencimiento = fecha de creación + plazo según tipo: `CERTIFICADO_ALUMNO_REGULAR` 2 días, `CAMBIO_COMISION` 3 días, `REVISION_EXAMEN` 5 días, `OTRO` 7 días, `EQUIVALENCIA` 15 días. Si la prioridad es `ALTA`, el plazo se reduce a la mitad (redondeo hacia abajo, mínimo 1 día). |
| **RN-07** | Transiciones válidas de estado: `PENDIENTE → EN_ANALISIS`, `PENDIENTE → CANCELADA`, `EN_ANALISIS → RESUELTA`, `EN_ANALISIS → RECHAZADA`. Cualquier otra → 409. |
| **RN-08** | Para pasar a `RESUELTA` o `RECHAZADA` la solicitud debe tener responsable asignado. Si no → 409. |
| **RN-09** | Solo se puede asignar un responsable **activo** y existente (si no existe → 404; si está inactivo → 409). No se puede asignar responsable a una solicitud cerrada (`RESUELTA`, `RECHAZADA`, `CANCELADA`) → 409. |
| **RN-10** | Al asignar un responsable a una solicitud `PENDIENTE`, esta pasa automáticamente a `EN_ANALISIS`. |
| **RN-11** | Una solicitud está **vencida** si no está cerrada y su fecha de vencimiento es anterior a ahora. |
| **RN-12** | Al cambiar el estado se registra la fecha de última actualización. Al pasar a `RESUELTA`/`RECHAZADA` se "notifica" al estudiante (en este alcance: una línea de log). |

## 6. Requerimientos no funcionales (calidad externa)

| ID | Requerimiento |
|---|---|
| RNF-01 | Respuestas en JSON; errores con el código HTTP correcto (400, 404, 409). |
| RNF-02 | La API se puede explorar y probar desde Swagger UI sin herramientas extra. |
| RNF-03 | Arranca con un solo comando y con datos de prueba precargados. |
| RNF-04 | Solo datos sintéticos. |

## 7. Requerimientos de calidad interna (objetivo de la v2)

| ID | Objetivo |
|---|---|
| RCI-01 | Cada clase con una sola responsabilidad (SRP). |
| RCI-02 | Reglas de negocio en el dominio, no en el controlador. |
| RCI-03 | Agregar un tipo de solicitud nuevo sin modificar la lógica de estados (OCP). |
| RCI-04 | Dependencias hacia abstracciones (repositorios, notificador, reloj) (DIP). |
| RCI-05 | Sin strings mágicos para estado, tipo y prioridad. |
| RCI-06 | Manejo de errores centralizado. |
| RCI-07 | Lógica de dominio testeable con tests unitarios sin levantar Spring. |

## 8. Criterios de aceptación globales
- Los 15 casos de prueba de caracterización (CP-01..CP-15, ver `05-esquema-backend.md` §6) pasan **en v1 y en v2**.
- La v2 resuelve al menos 8 de los smells del catálogo S01–S12.
- PMD reporta **menos violaciones** en v2 que en v1 (evidencia cuantitativa).
