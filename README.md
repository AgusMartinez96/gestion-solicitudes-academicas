# Gestión de Solicitudes Académicas

API REST en **Java 21 + Spring Boot 4.1.2** para registrar y seguir trámites académicos de estudiantes (certificados, revisiones de examen, equivalencias, cambios de comisión). Es el sistema base del **Trabajo Práctico Integrador de Metodología de Sistemas II** (Tecnicatura Universitaria en Programación a Distancia, UTN).

El trabajo **no evalúa cantidad de funcionalidad**: compara la **calidad interna** de dos versiones del mismo sistema con el **mismo contrato HTTP**:

- **v1 (`estado-inicial`)**: funciona bien desde afuera, pero con deuda técnica intencional (12 smells).
- **v2 (`estado-refactorizado`)**: el mismo comportamiento externo, con el diseño interno mejorado (SOLID y Clean Code), verificado con pruebas antes y después.

Los datos son sintéticos (nombres, legajos y emails inventados) y la base es H2 en memoria.

- **Autor:** Agustín Martínez · **Entrega individual**
- **Repositorio:** https://github.com/AgusMartinez96/gestion-solicitudes-academicas
- **Pull Request del refactoring:** [#1](https://github.com/AgusMartinez96/gestion-solicitudes-academicas/pull/1)

## Funcionalidades

| RF | Qué hace | Endpoint |
|---|---|---|
| RF-01 | Registrar una solicitud | `POST /api/solicitudes` |
| RF-02 | Consultar una solicitud | `GET /api/solicitudes/{id}` |
| RF-03 | Buscar por estado, tipo, prioridad y/o legajo | `GET /api/solicitudes?estado=&tipo=&prioridad=&legajo=` |
| RF-04 | Cambiar el estado respetando las transiciones | `PATCH /api/solicitudes/{id}/estado` |
| RF-05 | Asignar un responsable | `PATCH /api/solicitudes/{id}/responsable` |
| RF-06 | Reporte resumen (por estado, por tipo, vencidas) | `GET /api/reportes/resumen` |
| RF-07 | Listar estudiantes y responsables | `GET /api/estudiantes`, `GET /api/responsables` |

Códigos de respuesta: 200/201 (éxito), 400 (dato inválido), 404 (no encontrado), 409 (regla de negocio). Las reglas RN-01 a RN-12 están en [`docs/01-PRD.md`](docs/01-PRD.md) y el contrato JSON en [`docs/05-esquema-backend.md`](docs/05-esquema-backend.md).

## Tecnologías

| Componente | Versión / elección |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.2 |
| Persistencia | Spring Data JPA (Hibernate 7) + H2 en memoria |
| Documentación de API | springdoc-openapi 3.2.0 (Swagger UI) |
| Build | Maven con wrapper (`mvnw.cmd`) |
| Tests | JUnit 5 + MockMvc + AssertJ + Mockito |
| Cobertura | JaCoCo 0.8.16 |
| Análisis estático | PMD 7.17.0 (maven-pmd-plugin 3.28.0) |

## Requisitos

- **JDK 21** instalado y en el `PATH` (`java -version`).
- Windows con PowerShell o la terminal integrada de VS Code. No hace falta instalar Maven: se usa el wrapper.
- Git, para ver las versiones por tag.

## Instalación y uso

Desde la raíz del proyecto (donde está `pom.xml`), en PowerShell:

```powershell
git clone https://github.com/AgusMartinez96/gestion-solicitudes-academicas.git
cd gestion-solicitudes-academicas
.\mvnw.cmd spring-boot:run          # levanta la app en http://localhost:8080
```

| Acción | Comando |
|---|---|
| Compilar y correr los tests | `.\mvnw.cmd test` |
| Tests con compilación limpia | `.\mvnw.cmd clean test` |
| Reporte de cobertura (JaCoCo) | `.\mvnw.cmd clean test jacoco:report` → `target\site\jacoco\index.html` |
| Análisis estático (PMD) | `.\mvnw.cmd pmd:pmd` → `target\reports\pmd.html` |

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Consola H2:** http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:solicitudes`, usuario `sa`, sin contraseña)

En PowerShell hay que usar `curl.exe` (no `curl`, que es un alias de `Invoke-WebRequest`). Ejemplo:

```powershell
curl.exe -s http://localhost:8080/api/solicitudes/2
```

> La cobertura se mide siempre con `clean`: el agente de JaCoCo acumula datos entre ejecuciones si no se limpia `target\`.

## Ver la v1 y la v2

```powershell
git checkout estado-inicial          # v1: con deuda técnica (16 tests)
.\mvnw.cmd test

git checkout estado-refactorizado    # v2: refactorizada (105 tests)
.\mvnw.cmd test

git checkout main                    # volver a la rama principal
```

Los **tests de caracterización** (`src/test/.../api/`) son idénticos en ambas versiones y pasan 15/15 en las dos: es la prueba de que el comportamiento externo no cambió. La única diferencia externa permitida es el formato del cuerpo de error (texto plano en v1, JSON en v2; ver [ADR-003](docs/tpi/decisiones/ADR-003-cuerpo-de-error-json.md)).

## Tags del repositorio

| Tag | Qué marca |
|---|---|
| `estado-inicial` | **v1** con deuda técnica y tests de caracterización (commit `17fb47f`) |
| `etapa-diagnostico` | Diagnóstico de calidad y smells (commit `f4e57a6`) |
| `etapa-refactoring` | Merge del PR #1 con los refactorings R01–R09 y las correcciones del code review (commit `c7d1e40`) |
| `etapa-pruebas` | Pruebas unitarias, de integración y evidencias antes/después (commit `6fb02a7`) |
| `etapa-docs` | Documentación final de la Fase 7 (commit final del trabajo) |
| `estado-refactorizado` | **v2** definitiva (mismo commit que `etapa-docs`) |

`estadorefactorizado` es un alias del mismo commit con el nombre literal de la consigna.

## Métricas antes / después

| Métrica | v1 | v2 |
|---|---|---|
| Violaciones PMD | 56 | **9** (todas `ShortVariable` sobre `id`) |
| Líneas de `SolicitudController` | 433 | **100** (≈70 de lógica y 30 de anotaciones de OpenAPI) |
| Método más largo | 77 líneas | **15** |
| Tipos de producción | 11 | 39 |
| Tests | 16 | **105** |
| Cobertura de instrucciones / ramas (JaCoCo) | 82,2 % / 60,5 % | **95,5 % / 86,1 %** |
| Tests de caracterización | 15/15 | 15/15, sin modificar |

Detalle y método de medición en [`docs/tpi/06-pruebas-verificacion.md`](docs/tpi/06-pruebas-verificacion.md).

## Estructura de `docs/`

| Ruta | Contenido |
|---|---|
| `docs/01-PRD.md` … `06-plan-implementacion.md` | Documentación de partida: requisitos, TRD, flujos, diseño, esquema y plan por fases |
| `docs/tpi/01-sistema-base.md` | Descripción del sistema base (v1) |
| `docs/tpi/02-calidad-externa-interna.md` | Calidad externa vs. interna (ISO/IEC 25010) |
| `docs/tpi/03-deuda-tecnica-code-smells.md` | Los 12 smells con evidencia, prioridad y cuadrante de deuda |
| `docs/tpi/04-solid-clean-code.md` | SOLID y Clean Code: problema → mejora → refactoring → evidencia |
| `docs/tpi/refactorings.md` | Una ficha por refactoring (R01–R09) con antes/después y hash |
| `docs/tpi/06-pruebas-verificacion.md` | Estrategia de pruebas, métricas y cambios externos |
| `docs/tpi/07-code-review-checklist.md` | Checklist, 14 observaciones y decisiones |
| `docs/tpi/08-conclusiones.md` | Aprendizajes, límites y mejoras futuras |
| `docs/tpi/decisiones/` | ADR-001 a ADR-006 |
| `docs/tpi/pr-descripcion.md` | Texto del Pull Request #1 |
| `docs/tpi/informe-borrador.md` | Borrador completo del informe |
| `docs/tpi/guion-video.md` | Guion del video de 12 minutos |
| `docs/pruebas/casos-de-prueba.md` | Casos CP-01 a CP-15 con resultado v1 y v2 |
| `docs/evidencias/antes/`, `docs/evidencias/despues/` | Reportes (surefire, JaCoCo, PMD) y capturas |
| `docs/ia/registro-prompts.md` | Registro ordenado de los prompts usados |
| `docs/anexo-zip.md` | Lista de lo que va en el ZIP de anexos |

## Declaración de uso de IA

Se usó IA generativa en este trabajo, de forma declarada y con validación del autor:

- **Claude (chat, claude.ai):** planificación del TPI y armado de la documentación de partida y de los prompts por fase.
- **Claude Code (extensión de VS Code):** generación de la v1, de los tests de caracterización y de los refactorings; borradores de documentación; revisión asistida del Pull Request.

El registro ordenado de los prompts está en [`docs/ia/registro-prompts.md`](docs/ia/registro-prompts.md). El autor ejecutó los tests tras cada cambio, leyó el diff de cada commit, tomó las decisiones del code review (aceptar, diferir o rechazar cada observación) y es responsable de explicar y defender cada línea del trabajo.

## Autor

**Agustín Martínez** · Entrega individual · Metodología de Sistemas II, Tecnicatura Universitaria en Programación a Distancia, UTN.
