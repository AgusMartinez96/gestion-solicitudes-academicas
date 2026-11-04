# 08 — Conclusiones

Este documento reúne lo que se aprendió, lo que costó, lo que el trabajo **no** resuelve y por dónde seguiría. Las cifras provienen de los demás documentos de `docs/tpi/` y de los reportes en `docs/evidencias/`.

## 1. Resultado en una mirada

| | v1 (`estado-inicial`) | v2 (`estado-refactorizado`) |
|---|---|---|
| Comportamiento externo | 15/15 casos de caracterización | 15/15, **sin modificar** el test |
| Violaciones PMD | 56 | 9 (todas `ShortVariable` sobre `id`) |
| `SolicitudController` | 433 líneas | 100 (≈70 de lógica y 30 de anotaciones de OpenAPI) |
| Método más largo | 77 líneas | 15 |
| Tests | 16 | 105 |
| Cobertura de instrucciones / ramas | 82,2 % / 60,5 % | 95,5 % / 86,1 % |

El sistema hace lo mismo desde afuera; lo que cambió es cuánto cuesta entenderlo, probarlo y modificarlo. Agregar un tipo de solicitud pasó de **6 lugares en 2 archivos (con 2 fallas silenciosas)** a **una constante en un enum**.

## 2. Aprendizajes

1. **Calidad externa y calidad interna son cosas distintas.** La v1 era correcta y completa para quien la usaba, y aun así era cara de cambiar. Una cobertura de 82 % no decía nada sobre el diseño.
2. **El refactoring seguro necesita la red de seguridad antes, no después.** Los tests de caracterización se escribieron sobre la v1 y no se tocaron durante el refactoring: cada vez que algo se rompió, se corrigió el código de producción, no el test. Esa disciplina es la que permite afirmar "el comportamiento externo no cambió".
3. **Pasos chicos y un commit por técnica.** Mover o extraer primero, renombrar después, nunca reescribir de cero. Con 16 tests en verde después de cada paso, un error tenía como máximo un commit de antigüedad.
4. **Las reglas pertenecen al dominio.** Mover `puedeCambiar` a `EstadoSolicitud` (R03) y, tras el code review, las reglas de asignación a `Solicitud` (O-1) hizo que las reglas se pudieran probar sin Spring (63 tests de dominio).
5. **El code review encuentra lo que el autor no ve.** De 14 observaciones, 11 se corrigieron, 1 se difirió y 2 se rechazaron con justificación. Rechazar bien (O-12, O-13) fue tan importante como aceptar: se apoyó en el contrato HTTP, que es lo que no puede cambiar.
6. **Decir "no" a una dependencia es una decisión de diseño.** El TRD pedía Bean Validation; se comprobó que no garantizaba el orden ni los mensajes del contrato y se dejó la validación manual con su ADR (ADR-005).

## 3. Dificultades y hallazgos reales del proceso

| # | Hallazgo | Cuándo | Qué se hizo / qué se aprendió |
|---|---|---|---|
| 1 | **Columnas `enum` nativas de Hibernate 7.** Con `@Enumerated(EnumType.STRING)` solo, H2 generaba columnas `enum (...)` en lugar de `varchar`, cambiando el esquema del contrato. | R02 | Se agregó `@JdbcTypeCode(SqlTypes.VARCHAR)` y se verificó el DDL con `logging.level.org.hibernate.SQL=DEBUG`. Lección: un refactoring "inocente" puede cambiar la base; hay que mirar el esquema, no solo los tests. |
| 2 | **`LazyInitializationException` invisible para los tests transaccionales.** Al mover el mapeo fuera del service, `open-in-view=false` cerraba la sesión y leer `responsable` fallaba, pero los 16 tests seguían en verde. | R06 | Apareció recién al llamar por HTTP con la app levantada; se corrigió con `@EntityGraph`. En la Fase 6 se agregó un test de integración no transaccional y se comprobó que **falla** sin el grafo (4 de 11) mientras la caracterización sigue 15/15 (ADR-006). Lección: los tests deben poder fallar; un test que no detecta el defecto no protege. |
| 3 | **Cobertura de JaCoCo inflada sin `clean`.** La primera medición de la Fase 6 dio 97,0 % de instrucciones y 87,8 % de ramas; la real, con `clean`, es 95,5 % y 86,1 %. El agente acumulaba datos de corridas anteriores y el reporte arrastraba páginas de la v1. | Fase 6 | Se regeneró toda la evidencia y se corrigió el documento. Lección: una métrica solo vale si se sabe cómo se midió. |
| 4 | **`GodClass` de PMD no dispara.** El controller de 433 líneas, con WMC ≥ 100 y ATFD > 5, queda con TCC apenas por encima de 1/3 y la regla no salta. | Fase 3 | El diagnóstico se apoyó en tamaño, complejidad, conteo de responsabilidades y la captura de Swagger. Lección: la herramienta mide otra cosa que "responsabilidades mezcladas". |
| 5 | **6 de los 12 smells no los ve ninguna herramienta.** S03, S04, S06, S07, S08 y S11 no tienen violaciones PMD (la duplicación requeriría CPD, que no se usó). | Fase 3 | Las herramientas complementan, pero no reemplazan, la lectura del diseño. |
| 6 | **Springdoc perdió el 200 al declarar solo errores.** Con `@ApiResponse` para 400/404/409, Swagger dejó de mostrar el 200 en cuatro endpoints. | O-10 | Se declaró explícitamente `@ApiResponse(responseCode = "200")` y se verificó en `/v3/api-docs`. |
| 7 | **Spring Boot 4 no es Spring Boot 3.** Paquetes de test distintos, starters modulares, Jackson 3 y consola H2 como módulo aparte. | Fases 0–2 | Se verificó cada import compilando; el TRD §1.1 quedó como guía. |
| 8 | **Cifras del prompt que no coincidían con el código.** Un prompt anticipaba 110 líneas para el controller; la medición real fue 70 (y 100 después de las anotaciones de OpenAPI). | Fase 5 | Se usó siempre la cifra medida y se aclaró en los documentos. |

## 4. Límites del trabajo

- **Sin autenticación ni roles.** Los actores interactúan desde Swagger o `curl`; cualquiera puede cambiar cualquier solicitud.
- **H2 en memoria.** Los datos no persisten y no se contempla migración de esquema ni configuración por entorno (ADR-004).
- **Filtrado en memoria.** `buscar` lee todas las solicitudes y filtra en el service; no escala con el volumen (ADR-002).
- **O-13: una descripción de solo espacios es válida.** Diez espacios cumplen la regla RN-02 (longitud 10 a 500). Es un comportamiento **heredado de la v1**; aplicar `strip()` cambiaría el contrato y el TPI exige preservarlo. Se registra como límite conocido.
- **O-12 rechazada.** `listar` conserva 4 `@RequestParam` en el borde HTTP porque son el contrato; internamente el filtro ya es un Parameter Object.
- **Un solo cambio externo.** El formato del cuerpo de error (texto plano → JSON) y, después, tres mensajes de framework en español (O-9): declarados, no ocultos.
- **LSP e ISP con poca superficie.** Una interfaz de un método; no se inventaron abstracciones adicionales.
- **No se midió duplicación con CPD**; S03 se apoyó en lectura del código.
- **Datos y pruebas sintéticos.** No hubo usuarios reales ni pruebas de carga o de seguridad.

## 5. Mejoras futuras

1. **Filtrado y paginación en la base** con `Specification` o JPQL (ADR-002), manteniendo los 4 parámetros del borde HTTP.
2. **Autenticación y roles** (estudiante, responsable, coordinación) con control de quién puede cambiar qué.
3. **Persistencia real** (PostgreSQL o similar) con migraciones versionadas.
4. **Validación de la descripción con `strip()`** (O-13), como mejora intencional con su propio test y su nota de cambio de contrato.
5. **Notificación real** (mail) detrás de la interfaz `Notificador`, que ya está preparada para otra implementación.
6. **CPD y reglas de PMD adicionales** en el análisis para cubrir duplicación y otros smells hoy invisibles.
7. **Integración continua** que corra `clean test`, PMD y JaCoCo en cada Pull Request.
8. **Reevaluar Bean Validation** (ADR-005) si el contrato aceptara acumular errores.

## 6. Relación con los contenidos de la materia

| Contenido de Metodología de Sistemas II | Dónde aparece en el trabajo |
|---|---|
| Calidad de producto (ISO/IEC 25010): calidad externa vs. interna | `02-calidad-externa-interna.md` |
| Deuda técnica y su clasificación (cuadrante de Fowler) | `03-deuda-tecnica-code-smells.md` §5 |
| Code smells y su detección con herramientas y con lectura del diseño | `03-deuda-tecnica-code-smells.md` §2 y §4 |
| Principios SOLID y Clean Code | `04-solid-clean-code.md` |
| Refactoring seguro con catálogo (Extract Method, Move Method, Introduce Parameter Object, etc.) | `refactorings.md` |
| Pruebas de caracterización, unitarias y de integración; métricas antes/después | `06-pruebas-verificacion.md` |
| Revisión de código con checklist y gestión de observaciones | `07-code-review-checklist.md`, Pull Request #1 |
| Gestión de configuración: ramas, Pull Request, tags | README (tabla de tags) |
| Registro de decisiones técnicas | `decisiones/` (ADR-001 a ADR-006) |
| Uso responsable de IA con validación humana | `docs/ia/registro-prompts.md` |

_(El autor puede ajustar esta tabla con las unidades y los autores de la bibliografía de la cursada.)_

## 7. Conclusión individual

_(la escribe el autor)_

## 8. Justificación de la entrega individual

_(completa el autor)_
