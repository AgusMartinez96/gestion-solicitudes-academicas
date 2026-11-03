# 05 — Esquema backend: modelo, endpoints y contrato

> Todo lo de este documento es **contrato**: tiene que ser idéntico en v1 y en v2 (salvo el formato del cuerpo de error, ver §5.4).

## 1. Modelo de datos (tablas generadas por JPA)

### `estudiante`
| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK, identity |
| legajo | VARCHAR(20) | NOT NULL, UNIQUE |
| nombre | VARCHAR(100) | NOT NULL |
| email | VARCHAR(120) | NOT NULL |

### `responsable`
| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK, identity |
| nombre | VARCHAR(100) | NOT NULL |
| area | VARCHAR(60) | NOT NULL |
| activo | BOOLEAN | NOT NULL |

### `solicitud`
| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK, identity |
| tipo | VARCHAR(40) | NOT NULL (v1: String; v2: enum guardado como STRING) |
| descripcion | VARCHAR(500) | NOT NULL |
| prioridad | VARCHAR(10) | NOT NULL |
| estado | VARCHAR(20) | NOT NULL |
| fecha_creacion | TIMESTAMP | NOT NULL |
| fecha_vencimiento | TIMESTAMP | NOT NULL |
| fecha_actualizacion | TIMESTAMP | NULL |
| estudiante_id | BIGINT | FK → estudiante, NOT NULL |
| responsable_id | BIGINT | FK → responsable, NULL |

Relaciones: `Solicitud` N–1 `Estudiante`; `Solicitud` N–0..1 `Responsable` (`@ManyToOne(fetch = LAZY)`).

**Valores de dominio**
- Tipo: `CERTIFICADO_ALUMNO_REGULAR`, `REVISION_EXAMEN`, `EQUIVALENCIA`, `CAMBIO_COMISION`, `OTRO`
- Prioridad: `BAJA`, `MEDIA`, `ALTA`
- Estado: `PENDIENTE`, `EN_ANALISIS`, `RESUELTA`, `RECHAZADA`, `CANCELADA`

## 2. Fechas
- Toda fecha "ahora" se **trunca a segundos** (`truncatedTo(ChronoUnit.SECONDS)`).
- En JSON se serializan en ISO-8601 sin zona: `"2026-11-01T19:30:00"`.

## 3. Endpoints

| # | Método | Ruta | Body | Éxito | Errores |
|---|---|---|---|---|---|
| E1 | POST | `/api/solicitudes` | `{ "legajo", "tipo", "descripcion", "prioridad"? }` | **201** + Solicitud | 400, 404 |
| E2 | GET | `/api/solicitudes/{id}` | — | **200** + Solicitud | 404 |
| E3 | GET | `/api/solicitudes?estado=&tipo=&prioridad=&legajo=` | — | **200** + lista (orden por id ascendente) | 400 (valor de filtro inválido) |
| E4 | PATCH | `/api/solicitudes/{id}/estado` | `{ "estado" }` | **200** + Solicitud | 400, 404, 409 |
| E5 | PATCH | `/api/solicitudes/{id}/responsable` | `{ "responsableId" }` | **200** + Solicitud | 404, 409 |
| E6 | GET | `/api/reportes/resumen` | — | **200** + Reporte | — |
| E7 | GET | `/api/estudiantes` | — | **200** + lista | — |
| E8 | GET | `/api/responsables` | — | **200** + lista | — |

## 4. Datos sintéticos (`src/main/resources/data.sql`)

> **No** especificar `id` en los INSERT (la columna es identity): insertar **en este orden** para que los ids queden 1..n. Usar `DATEADD` de H2 para que las fechas sean relativas a "ahora" y los tests sean deterministas.

**Estudiantes**
| id | legajo | nombre | email |
|---|---|---|---|
| 1 | L-1001 | Lucía Fernández | lucia.fernandez@alumnos.example |
| 2 | L-1002 | Tomás Rivero | tomas.rivero@alumnos.example |
| 3 | L-1003 | Martina Sosa | martina.sosa@alumnos.example |

**Responsables**
| id | nombre | area | activo |
|---|---|---|---|
| 1 | Carla Gómez | Bedelía | true |
| 2 | Diego Paz | Departamento Alumnos | true |
| 3 | Hernán Ruiz | Secretaría Académica | **false** |

**Solicitudes**
| id | tipo | prioridad | estado | estudiante | responsable | fecha_creacion | fecha_vencimiento |
|---|---|---|---|---|---|---|---|
| 1 | CERTIFICADO_ALUMNO_REGULAR | MEDIA | PENDIENTE | 1 | — | ahora − 10 días | ahora − 8 días (**vencida**) |
| 2 | REVISION_EXAMEN | ALTA | EN_ANALISIS | 2 | 1 | ahora − 1 día | ahora + 1 día |
| 3 | EQUIVALENCIA | BAJA | RESUELTA | 1 | 2 | ahora − 20 días | ahora − 5 días |
| 4 | CAMBIO_COMISION | MEDIA | CANCELADA | 3 | — | ahora − 10 días | ahora − 7 días |
| 5 | OTRO | BAJA | PENDIENTE | 3 | — | ahora − 1 día | ahora + 6 días |

Descripciones: textos sintéticos de más de 10 caracteres (por ejemplo, "Necesito el certificado para presentar en el trabajo").

Ejemplo de INSERT:
```sql
INSERT INTO solicitud (tipo, descripcion, prioridad, estado, fecha_creacion, fecha_vencimiento, estudiante_id, responsable_id)
VALUES ('CERTIFICADO_ALUMNO_REGULAR', 'Necesito el certificado para presentar en el trabajo', 'MEDIA', 'PENDIENTE',
        DATEADD('DAY', -10, CURRENT_TIMESTAMP), DATEADD('DAY', -8, CURRENT_TIMESTAMP), 1, NULL);
```

## 5. Contrato JSON

### 5.1 Solicitud
```json
{
  "id": 6,
  "tipo": "REVISION_EXAMEN",
  "descripcion": "Solicito revisión del parcial de Programación III",
  "prioridad": "ALTA",
  "estado": "PENDIENTE",
  "fechaCreacion": "2026-11-01T19:30:00",
  "fechaVencimiento": "2026-11-03T19:30:00",
  "fechaActualizacion": null,
  "vencida": false,
  "estudiante": { "legajo": "L-1002", "nombre": "Tomás Rivero" },
  "responsable": null
}
```
Con responsable: `"responsable": { "id": 1, "nombre": "Carla Gómez", "area": "Bedelía" }`.

### 5.2 Reporte resumen
```json
{
  "total": 5,
  "porEstado": { "PENDIENTE": 2, "EN_ANALISIS": 1, "RESUELTA": 1, "RECHAZADA": 0, "CANCELADA": 1 },
  "porTipo": { "CERTIFICADO_ALUMNO_REGULAR": 1, "REVISION_EXAMEN": 1, "EQUIVALENCIA": 1, "CAMBIO_COMISION": 1, "OTRO": 1 },
  "vencidas": 1
}
```
Las claves de `porEstado` y `porTipo` van **siempre todas**, en el orden de arriba.

### 5.3 Estudiante / Responsable
```json
{ "id": 1, "legajo": "L-1001", "nombre": "Lucía Fernández", "email": "lucia.fernandez@alumnos.example" }
{ "id": 1, "nombre": "Carla Gómez", "area": "Bedelía", "activo": true }
```

### 5.4 Errores
**Mensajes exactos** (los tests verifican que el cuerpo **contenga** el mensaje):

| Situación | HTTP | Mensaje |
|---|---|---|
| Descripción nula, vacía, < 10 o > 500 | 400 | `La descripción debe tener entre 10 y 500 caracteres` |
| Tipo inválido o ausente | 400 | `Tipo de solicitud inválido: {valor}` |
| Prioridad inválida | 400 | `Prioridad inválida: {valor}` |
| Estado inválido (body o filtro) | 400 | `Estado inválido: {valor}` |
| Legajo inexistente | 404 | `Estudiante no encontrado: {legajo}` |
| Solicitud inexistente | 404 | `Solicitud no encontrada: {id}` |
| Responsable inexistente | 404 | `Responsable no encontrado: {id}` |
| Transición no permitida | 409 | `Transición inválida: {origen} -> {destino}` |
| Cierre sin responsable | 409 | `La solicitud requiere un responsable asignado para pasar a {destino}` |
| Responsable inactivo | 409 | `El responsable no está activo: {id}` |
| Asignar a solicitud cerrada | 409 | `La solicitud está cerrada: {id}` |

**Formato del cuerpo**
- v1: texto plano `Error: {mensaje}` con `Content-Type: text/plain;charset=UTF-8`.
- v2: JSON `{ "status": 409, "error": "Conflict", "mensaje": "...", "timestamp": "..." }`.

## 6. Casos de prueba de caracterización (CP-01 … CP-15)

Clase: `src/test/java/ar/edu/utn/tup/solicitudes/api/SolicitudesApiCaracterizacionTest.java`
- `@SpringBootTest` + `@AutoConfigureMockMvc` + **`@Transactional`** (cada test hace rollback, así los datos semilla quedan iguales para todos).
- Leer los cuerpos con `result.getResponse().getContentAsString(StandardCharsets.UTF_8)` (evita problemas con tildes).
- Un `@DisplayName` por test con el formato `"CP-01 — ..."`.
- **Solo** se usan las rutas HTTP: los tests no conocen ninguna clase interna. Por eso sirven igual para v1 y v2.

| ID | Requerimiento | Acción | Resultado esperado |
|---|---|---|---|
| CP-01 | RF-01, RN-05, RN-06 | POST CAMBIO_COMISION, prioridad MEDIA, legajo L-1001 | 201; estado PENDIENTE; prioridad MEDIA; vencimiento − creación = 3 días |
| CP-02 | RN-04, RN-06 | POST REVISION_EXAMEN **sin** prioridad, L-1002 | 201; prioridad ALTA; vencimiento − creación = 2 días (5 / 2) |
| CP-03 | RN-02 | POST con descripción `"corta"` | 400; contiene `La descripción debe tener entre 10 y 500 caracteres` |
| CP-04 | RN-03 | POST con tipo `"BECA"` | 400; contiene `Tipo de solicitud inválido: BECA` |
| CP-05 | RN-01 | POST con legajo `"L-9999"` | 404; contiene `Estudiante no encontrado: L-9999` |
| CP-06 | RF-02 | GET `/api/solicitudes/2` | 200; estado EN_ANALISIS; responsable.nombre = Carla Gómez |
| CP-07 | RF-02 | GET `/api/solicitudes/99` | 404; contiene `Solicitud no encontrada: 99` |
| CP-08 | RF-03 | GET `?estado=PENDIENTE` → ids [1,5]; GET `?estado=PENDIENTE&legajo=L-1003` → ids [5]; GET `?estado=FOO` → 400 | según lo indicado |
| CP-09 | RF-04, RN-07, RN-12 | PATCH estado de 5 a EN_ANALISIS | 200; estado EN_ANALISIS; fechaActualizacion no nula |
| CP-10 | RN-07 | PATCH estado de 4 (CANCELADA) a EN_ANALISIS; PATCH estado de 1 a `"FOO"` | 409 `Transición inválida: CANCELADA -> EN_ANALISIS`; 400 `Estado inválido: FOO` |
| CP-11 | RN-08 | PATCH 1 a EN_ANALISIS (200) y después a RESUELTA | 409; contiene `requiere un responsable asignado` |
| CP-12 | RN-07, RN-12 | PATCH estado de 2 a RESUELTA | 200; estado RESUELTA |
| CP-13 | RF-05, RN-10 | PATCH responsable 1 en solicitud 5 | 200; estado EN_ANALISIS; responsable.id = 1 |
| CP-14 | RN-09 | PATCH responsable 3 (inactivo) en solicitud 5; PATCH responsable 1 en solicitud 3 (cerrada); responsable 99 | 409 / 409 / 404 con los mensajes de §5.4 |
| CP-15 | RF-06, RN-11 | GET `/api/reportes/resumen` | 200; total 5; PENDIENTE 2; EN_ANALISIS 1; RESUELTA 1; RECHAZADA 0; CANCELADA 1; cada tipo 1; vencidas 1 |

Además, en v2 se agregan **tests unitarios de dominio** (sin Spring) en `src/test/java/.../dominio/`:
- `EstadoSolicitudTest`: tabla completa de transiciones válidas e inválidas (`@ParameterizedTest`).
- `TipoSolicitudTest`: plazo y prioridad por defecto de cada tipo.
- `SolicitudTest`: `registrar`, `cambiarEstado`, `asignarResponsable`, `estaVencida` usando una fecha fija.
- `SolicitudServiceTest`: con `Clock.fixed(...)` y un `NotificadorFake` para verificar que se notifica al cerrar (muestra el beneficio de DIP).
