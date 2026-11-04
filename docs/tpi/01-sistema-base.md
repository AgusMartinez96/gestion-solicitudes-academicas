# 01 — Sistema base (v1)

## 1. Dominio y problema

En una facultad, los estudiantes hacen trámites (certificados, revisiones de examen, equivalencias, cambios de comisión) por mail o en ventanilla. No hay trazabilidad: no se sabe en qué estado está cada pedido, quién lo atiende ni cuáles están vencidos.

El **Sistema de Gestión de Solicitudes Académicas** centraliza esas solicitudes: permite registrarlas, clasificarlas, asignarlas a un responsable, cambiar su estado, consultarlas y obtener un reporte simple.

El foco del trabajo no es la cantidad de funcionalidad, sino comparar la **calidad interna** de dos versiones del mismo sistema: la **v1** (esta, con deuda técnica) y la **v2** (refactorizada), ambas con el mismo contrato HTTP.

## 2. Alcance

**Dentro del alcance**
- Registro, consulta, búsqueda con filtros, cambio de estado y asignación de responsable de solicitudes.
- Reporte resumen (por estado, por tipo y vencidas).
- Listado de estudiantes y responsables.
- Persistencia en H2 en memoria con datos sintéticos precargados.
- Documentación de la API con Swagger UI.

**Fuera del alcance:** login y roles, frontend propio, notificaciones reales por mail (se simula con una línea de consola) y adjuntos.

## 3. Actores

| Actor | Descripción | Interacción |
|---|---|---|
| Estudiante | Inicia el trámite; se identifica por legajo. | Registra solicitudes y consulta las suyas. |
| Responsable | Personal de un área (Bedelía, Departamento Alumnos, Secretaría Académica). | Toma solicitudes y cambia su estado. |
| Coordinación | Rol de supervisión. | Asigna responsables y consulta el reporte. |

No hay autenticación: los actores interactúan con la API desde Swagger UI o curl.

## 4. Funcionalidades (RF ↔ endpoint)

| RF | Requerimiento | Endpoint | Éxito | Errores |
|---|---|---|---|---|
| RF-01 | Registrar una solicitud | `POST /api/solicitudes` | 201 | 400, 404 |
| RF-02 | Consultar una solicitud por id | `GET /api/solicitudes/{id}` | 200 | 404 |
| RF-03 | Buscar por estado, tipo, prioridad y/o legajo | `GET /api/solicitudes?estado=&tipo=&prioridad=&legajo=` | 200 | 400 |
| RF-04 | Cambiar el estado respetando las transiciones | `PATCH /api/solicitudes/{id}/estado` | 200 | 400, 404, 409 |
| RF-05 | Asignar un responsable | `PATCH /api/solicitudes/{id}/responsable` | 200 | 404, 409 |
| RF-06 | Reporte resumen | `GET /api/reportes/resumen` | 200 | — |
| RF-07 | Listar estudiantes y responsables | `GET /api/estudiantes`, `GET /api/responsables` | 200 | — |

Las reglas de negocio (RN-01 a RN-12) están en `docs/01-PRD.md` §5 y el contrato JSON en `docs/05-esquema-backend.md`.

## 5. Tecnología

| Componente | Elección |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.2 |
| Build | Maven con wrapper (`mvnw.cmd`) |
| Persistencia | Spring Data JPA + H2 en memoria |
| Documentación de API | springdoc-openapi 3.2.0 (Swagger UI) |
| Tests | JUnit 5 + MockMvc + AssertJ |
| Cobertura | JaCoCo 0.8.16 |
| Análisis estático | PMD 7.17.0 (maven-pmd-plugin 3.28.0) |

## 6. Cómo ejecutarlo

Desde la raíz del proyecto, en la terminal de VS Code (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run     # levanta la app en http://localhost:8080
.\mvnw.cmd test                # corre los tests (16 en la v1)
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Consola H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:solicitudes`, usuario `sa`, sin contraseña)

Para ver exactamente esta versión: `git checkout estado-inicial`.

## 7. Repositorio

https://github.com/AgusMartinez96/gestion-solicitudes-academicas

## 8. Evidencias de funcionamiento

Capturas manuales de la v1 en `docs/evidencias/antes/capturas/`:

| Captura | Qué muestra |
|---|---|
| `01-swagger-endpoints.png` | Swagger UI con los 8 endpoints (E1–E6 dentro de `solicitud-controller`, E7–E8 en `soporte-controller`). |
| `02-post-201.png` | Registro de una solicitud: 201 y JSON del contrato. |
| `03-error-409.png` | Transición inválida: 409 con cuerpo de error en texto plano. |
| `04-reporte.png` | Reporte resumen. |
| `05-notificacion-consola.png` | Notificación simulada por consola al resolver una solicitud. |

Además, 15 tests de caracterización (CP-01 a CP-15) pasan sobre la v1; el detalle está en `docs/pruebas/casos-de-prueba.md` y los reportes en `docs/evidencias/antes/` (`surefire/`, `jacoco/`, `pmd/`).

**Estado de la v1 (tag `estado-inicial`):** 16 tests en verde (15 de caracterización + `contextLoads`), cobertura JaCoCo de 82 % en instrucciones y 60 % en ramas, y 56 violaciones PMD.
