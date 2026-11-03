# 06 — Plan de implementación por fases

> Claude Code ejecuta **una fase por prompt** y se detiene. Cada fase indica: objetivo, tareas, commits, tag (si corresponde) y criterio de "fase terminada".
> Los tags los crea Claude Code **solo cuando el prompt de la fase lo pide**.

## Mapa de fases ↔ etapas del TPI ↔ tags

| Fase | Qué se hace | Etapa del TPI | Tag al final |
|---|---|---|---|
| 0 | Configurar el proyecto (pom, plugins, estructura de docs) | — | — |
| 1 | Construir la **v1 con deuda** | 1. Sistema base | — |
| 2 | Tests de caracterización CP-01..CP-15 en verde sobre v1 | 6. Pruebas (antes) | `estado-inicial` |
| 3 | Diagnóstico: calidad externa/interna, 8+ smells, PMD antes | 2 y 3 | `etapa-diagnostico` |
| 4 | Refactorings R01..R09 en la rama `refactor/solicitudes` | 4 y 5 | — |
| 5 | Pull Request + code review con checklist + correcciones + merge | 7 | `etapa-refactoring` |
| 6 | Tests unitarios nuevos + JaCoCo/PMD después + comparación | 6 | `etapa-pruebas` |
| 7 | README, ADRs, SOLID, conclusiones, borrador de informe y guion del video | 1, 4, 8 | `etapa-docs` y `estado-refactorizado` |

> **Por qué `estado-inicial` va después de la Fase 2:** el sistema base se congela junto con su red de seguridad (tests de caracterización), **sin tocar el código de producción de la v1**. Así cualquiera puede hacer `git checkout estado-inicial` y ejecutar `.\mvnw.cmd test` para ver que la v1 pasa los 15 casos. Esto se explica en el informe.

---

## Fase 0 — Configuración

**Precondición:** el autor ya generó el proyecto en start.spring.io (ver la guía) y copió `CLAUDE.md` y `docs/` a la raíz.

Tareas:
1. Revisar el `pom.xml` y agregar:
   - `org.springdoc:springdoc-openapi-starter-webmvc-ui` (línea **3.x**, la compatible con Spring Boot 4; ver `02-TRD.md` §1.1).
   - Plugin `jacoco-maven-plugin` 0.8.12 con goals `prepare-agent` y `report` (fase `test`).
   - Plugin `maven-pmd-plugin` (3.24.x) apuntando a `config/pmd/ruleset.xml`, con `failOnViolation=false`, `linkXRef=false` y reporte HTML.
2. Crear `config/pmd/ruleset.xml` con las reglas de `02-TRD.md` §4.
3. Escribir `application.properties` según `02-TRD.md` §3.
4. Crear la estructura vacía: `docs/tpi/`, `docs/tpi/decisiones/`, `docs/evidencias/antes/`, `docs/evidencias/despues/`, `docs/ia/` (con un `.gitkeep` donde haga falta).
5. Crear `.gitignore` estándar de Maven/IntelliJ/VS Code (`target/`, `.idea/`, `.vscode/`, `*.iml`).
6. Verificar que `.\mvnw.cmd test` compile (el test por defecto de Spring tiene que pasar).

Commits: `chore: configuración inicial del proyecto (springdoc, jacoco, pmd)`

Fase terminada cuando: `.\mvnw.cmd test` → BUILD SUCCESS.

---

## Fase 1 — Construcción de la v1 (sistema base con deuda)

Seguir **al pie de la letra** `04-diseno.md` §1 y §1.1 y el contrato de `05-esquema-backend.md`.

Tareas:
1. Entidades anémicas en `model/` (String para tipo, estado y prioridad).
2. Repositorios en `repository/`.
3. `util/Utils.java` (static) y `util/NotificadorConsola.java`.
4. `controller/SolicitudController.java` con E1–E6 y todos los smells S01–S12.
5. `controller/SoporteController.java` con E7–E8.
6. `data.sql` con los datos de `05-esquema-backend.md` §4.
7. README mínimo: qué es y cómo se ejecuta.

Commits sugeridos (para un historial realista, de 3 a 5 commits):
- `feat: entidades y repositorios de solicitudes, estudiantes y responsables`
- `feat: registro, consulta y búsqueda de solicitudes`
- `feat: cambio de estado y asignación de responsable`
- `feat: reporte resumen y datos de prueba`

Verificación manual (la hace el autor y saca capturas para `docs/evidencias/antes/`):
- `.\mvnw.cmd spring-boot:run`, abrir Swagger UI y probar E1–E8.

Fase terminada cuando: la app levanta y los 8 endpoints responden según el contrato.

---

## Fase 2 — Tests de caracterización sobre la v1

Tareas:
1. Crear `SolicitudesApiCaracterizacionTest` con CP-01..CP-15 (`05-esquema-backend.md` §6).
2. Si algún test falla, **se corrige la v1** para que cumpla el contrato, sin limpiar los smells.
3. Ejecutar `.\mvnw.cmd test jacoco:report` y `.\mvnw.cmd pmd:pmd`.
4. Copiar a `docs/evidencias/antes/`:
   - `target/surefire-reports/*.txt` → `docs/evidencias/antes/surefire/`
   - `target/site/jacoco/` → `docs/evidencias/antes/jacoco/`
   - `target/site/pmd.html` y `target/pmd.xml` → `docs/evidencias/antes/pmd/`
   (En Windows/PowerShell usar `Copy-Item -Recurse`.)
5. Crear `docs/pruebas/casos-de-prueba.md` con la tabla CP y una columna "Resultado v1" = ✅.

Commits:
- `test: tests de caracterización CP-01 a CP-15 sobre la API`
- `docs: evidencias de ejecución de la versión inicial`

**Tag:** `git tag -a estado-inicial -m "Sistema base v1 con tests de caracterización"`.

Fase terminada cuando: 15/15 tests en verde y el tag creado.

---

## Fase 3 — Diagnóstico (calidad y deuda técnica)

Tareas: redactar en `docs/tpi/`:

1. **`01-sistema-base.md`**: dominio, alcance, actores, funcionalidades (tabla RF ↔ endpoint), tecnología, cómo ejecutarlo, enlace al repo (dejar `{{URL_REPO}}`), evidencias de funcionamiento (referencias a capturas).
2. **`02-calidad-externa-interna.md`**:
   - Tabla "Calidad externa" (lo que percibe el usuario): funcionalidad completa, códigos HTTP correctos, datos de prueba, Swagger → ✅. Relacionar con ISO/IEC 25010 (adecuación funcional, usabilidad).
   - Tabla "Calidad interna" (lo que ve el equipo): mantenibilidad, modularidad, analizabilidad, modificabilidad, capacidad de ser probado (subcaracterísticas de mantenibilidad de ISO/IEC 25010) → problemas concretos con referencia a S01–S12.
   - Conclusión: "funciona pero es caro de cambiar", con un **ejemplo de costo del cambio**: listar todos los archivos y líneas que habría que tocar para agregar un tipo nuevo `BECA` en la v1.
3. **`03-deuda-tecnica-code-smells.md`**: una ficha por smell S01–S12 con:
   - Nombre del catálogo · **Evidencia** (fragmento de código real copiado de v1) · **Ubicación** (`archivo:línea`) · **Impacto** · **Riesgo de mantenimiento** · **Prioridad** (Alta/Media/Baja) · **Refactoring propuesto** (R0x) · **Detectado por PMD** (regla, si aplica).
   - Matriz final de priorización (impacto × esfuerzo).
   - Resumen de PMD: cantidad total de violaciones por regla (tabla), tomada de `docs/evidencias/antes/pmd/`.
   - Clasificación de la deuda según el cuadrante de Fowler (imprudente/prudente × deliberada/inadvertida), con justificación.

Commit: `docs: diagnóstico de calidad, deuda técnica y code smells`
**Tag:** `etapa-diagnostico`.

---

## Fase 4 — Refactoring seguro (rama `refactor/solicitudes`)

Antes de empezar: `git checkout -b refactor/solicitudes`.

Ejecutar los refactorings de `04-diseno.md` §4 **en orden**, en tres tandas (un prompt por tanda):
- **4a:** R01 Extract Method · R02 Replace Type Code with Enum · R03 Move Method
- **4b:** R04 Replace Conditional with Polymorphism · R05 Introduce Parameter Object · R06 Extract Class
- **4c:** R07 Excepciones + manejo centralizado · R08 Introduce Interface + DI (Notificador, Clock) · R09 Rename + constantes + remove dead code

Protocolo **por cada** refactoring:
1. Aplicar el cambio mínimo.
2. `.\mvnw.cmd test` → tiene que dar 15/15 en verde. **Prohibido tocar `src/test/.../api/`.**
3. Agregar la ficha en `docs/tpi/refactorings.md`:
   ```
   ## R0x — <Técnica>
   - Smell(s): S0x · Principio: <SRP/OCP/DIP/Clean Code>
   - Motivación: …
   - Antes (archivo:líneas, fragmento) …
   - Después (archivo:líneas, fragmento) …
   - Pasos seguros aplicados: 1… 2… 3…
   - Verificación: `mvnw test` → Tests run: 15, Failures: 0
   - Commit: <hash corto>
   ```
4. Commit: `refactor(R0x): <descripción breve>`.

Al final de la tanda 4c, la estructura tiene que coincidir con `04-diseno.md` §3 y no deben quedar `Utils`, `Map<String,Object>`, `catch (Exception)` ni `System.out`.

---

## Fase 5 — Code review profesional

Tareas:
1. Crear `docs/tpi/07-code-review-checklist.md` con el checklist (ver abajo), en formato tabla: `# · Criterio · Cómo se verifica · Resultado (✅/⚠️/❌) · Observación · Acción`.
2. `git push -u origin refactor/solicitudes` y abrir el PR hacia `main` (con `gh pr create` si está `gh` instalado; si no, indicar al autor cómo hacerlo desde la web).
   - Título: `Refactoring v2: SOLID, Clean Code y manejo de errores`.
   - Descripción: resumen de R01–R09, cómo se verificó (tests 15/15) y enlace al checklist.
3. **Revisión**: aplicar el checklist sobre el diff del PR y producir **mínimo 6 observaciones** reales (cosas que todavía se puedan mejorar, no elogios) con formato de feedback profesional:
   `[Severidad: bloqueante/mayor/menor/sugerencia] archivo:línea — Observación objetiva — Por qué importa — Propuesta`.
   Listarlas en `docs/tpi/07-code-review-checklist.md` §Observaciones, indicando que la revisión fue asistida por IA y validada por el autor.
4. El autor las publica como comentarios en el PR (o Claude Code con `gh pr review --comment` / `gh api`).
5. Corregir las observaciones aceptadas, una por commit: `fix(review): <descripción>`. Las rechazadas se justifican.
6. Criterios de aceptación del PR (todos ✅ para mergear):
   - 15/15 tests de caracterización en verde sin modificaciones.
   - Sin violaciones PMD de GodClass, ExcessiveParameterList ni AvoidCatchingGenericException.
   - Ningún método de producción con más de 20 líneas.
   - Observaciones bloqueantes y mayores resueltas.
   - `refactorings.md` completo con hashes.
7. Merge del PR (merge commit, no squash, para conservar los commits por refactoring).
8. `git checkout main` y `git pull`.

**Tag:** `etapa-refactoring` (sobre el merge commit en `main`).

### Checklist base (Claude Code lo adapta y completa)
| Área | Criterios |
|---|---|
| A. Contrato y funcionalidad | Rutas, JSON y códigos HTTP iguales a v1 · tests de caracterización intactos y en verde · el único cambio externo es el formato de error (documentado) |
| B. Diseño / SOLID | Una responsabilidad por clase · controllers sin lógica de negocio · reglas en el dominio · dependencias por constructor e interfaces · sin `new` de colaboradores |
| C. Clean Code | Nombres que revelan intención · métodos ≤ 20 líneas · ≤ 3 parámetros · sin números ni strings mágicos · sin código muerto ni comentarios redundantes |
| D. Manejo de errores | Excepciones específicas · sin `catch (Exception)` · un solo `@RestControllerAdvice` · mensajes claros y consistentes |
| E. Tests | Tests unitarios del dominio · nombres descriptivos · sin dependencia de la hora real |
| F. Datos y seguridad | Solo datos sintéticos · sin secretos en el repo |
| G. Git y documentación | Un commit por refactoring · mensajes claros · `refactorings.md` y README actualizados |

---

## Fase 6 — Pruebas y verificación

Tareas:
1. Agregar los tests unitarios de dominio y del service (`05-esquema-backend.md` §6, último bloque) en `src/test/.../dominio/` y `src/test/.../aplicacion/`.
2. `.\mvnw.cmd test jacoco:report` y `.\mvnw.cmd pmd:pmd`; copiar los reportes a `docs/evidencias/despues/` (misma estructura que en "antes").
3. Redactar `docs/tpi/06-pruebas-verificacion.md`:
   - Estrategia: tests de caracterización (caja negra) como red de seguridad + unitarios nuevos.
   - Tabla CP-01..CP-15 con columnas **Resultado v1 (`estado-inicial`)** y **Resultado v2**.
   - Cómo reproducirlo: `git checkout estado-inicial` → `.\mvnw.cmd test` → `git checkout main` → `.\mvnw.cmd test`.
   - **Tabla de métricas antes/después**: total de violaciones PMD; violaciones por regla; líneas de `SolicitudController`; método más largo (líneas); cantidad de clases; cantidad de tests; cobertura de líneas (JaCoCo).
   - Cambio externo intencional (cuerpo de error) con ejemplo antes/después.
4. Actualizar `docs/pruebas/casos-de-prueba.md` con la columna v2.

Commit: `test: tests unitarios de dominio y evidencias de verificación`
**Tag:** `etapa-pruebas`.

---

## Fase 7 — Documentación final y cierre

Tareas:
1. **README.md** completo: descripción, funcionalidades, tecnologías, requisitos, instalación y uso (comandos de Windows), endpoints, cómo ver v1 y v2 (`git checkout <tag>`), estructura de `docs/`, tabla de tags, declaración de uso de IA y autor.
2. **`docs/tpi/04-solid-clean-code.md`**: por cada principio SOLID y cada práctica de Clean Code (nombres, funciones, errores, cohesión, acoplamiento, límites entre componentes): problema en v1 → mejora en v2 → refactoring → evidencia (archivo). Incluir la tabla de honestidad de `04-diseno.md` §5 y diagramas de clases antes/después en Mermaid.
3. **ADRs** `docs/tpi/decisiones/ADR-001..004` (formato: Contexto · Decisión · Alternativas · Consecuencias).
4. **`docs/tpi/08-conclusiones.md`**: borrador con aprendizajes, dificultades, límites del trabajo (por ejemplo: sin autenticación, H2, filtrado en memoria, alcance chico), mejoras futuras y relación con los contenidos de la materia. Dejar una sección **"Conclusión individual"** con `_(la escribe el autor)_`, y la **justificación de la entrega individual** con `_(completa el autor)_`.
5. **`docs/tpi/informe-borrador.md`**: informe completo que **integra** todo, siguiendo la estructura de la consigna §11: portada (materia, comisión `{{COMISION}}`, título, autor, fecha) · resumen ejecutivo (5–7 líneas) · sistema base · calidad externa/interna · deuda y smells · SOLID/Clean Code · refactorings antes/después · pruebas · code review · conclusiones · declaración de uso de IA (herramienta, finalidad, grado de intervención) · enlaces (`{{URL_REPO}}`, `{{URL_VIDEO}}`) · anexos. Marcar con `📸 [CAPTURA: …]` dónde va cada captura.
6. **`docs/tpi/guion-video.md`**: guion de 12 minutos con los bloques de la consigna, tiempo por bloque, qué mostrar en pantalla (archivo/commit/test) y qué decir (en viñetas, para hablar con voz propia, sin leer).
7. **`docs/anexo-zip.md`**: lista de qué meter en el ZIP de anexos (evidencias antes/después, capturas, diagramas, reportes PMD/JaCoCo, `registro-prompts.md`, export de conversaciones con IA).
8. Revisar que `docs/ia/registro-prompts.md` tenga todas las fases.

Commits: `docs: README, decisiones técnicas, conclusiones e informe`
**Tags:** `etapa-docs` y luego `estado-refactorizado` (en el commit final).
Push: `git push origin main --tags`.
