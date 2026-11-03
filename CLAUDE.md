# CLAUDE.md — TPI Metodología de Sistemas II

> Punto de entrada para Claude Code. Leer este archivo completo antes de cualquier tarea y después leer los documentos de `docs/` en el orden indicado abajo.

## 1. Qué es este proyecto

**Sistema de Gestión de Solicitudes Académicas**: API REST en Java + Spring Boot. Es el sistema base del **Trabajo Práctico Integrador (TPI) de Metodología de Sistemas II** (UTN – Tecnicatura Universitaria en Programación a Distancia).

Autor: **Agustín Martínez**. Entrega **individual**.

El TPI **no evalúa cantidad de funcionalidad**. Evalúa:
1. diagnóstico de calidad externa vs. interna,
2. deuda técnica y code smells (mínimo 8),
3. aplicación de SOLID y Clean Code,
4. refactoring seguro (mínimo 4, entre ellos Extract Method, Move Method e Introduce Parameter Object),
5. pruebas antes/después que demuestren que **el comportamiento externo no cambió**,
6. code review con checklist.

El proyecto tiene **dos versiones** en el mismo repositorio, separadas por tags de Git:
- `estado-inicial` → **v1**: funciona bien desde afuera, pero tiene deuda técnica **intencional y realista**.
- `estado-refactorizado` → **v2**: el mismo contrato HTTP, con el diseño interno mejorado.

## 2. Orden de lectura de la documentación

1. `docs/01-PRD.md` — qué hace el sistema (requerimientos funcionales y reglas de negocio).
2. `docs/02-TRD.md` — stack, convenciones y requisitos técnicos.
3. `docs/03-flujo.md` — flujos de uso y máquina de estados de la solicitud.
4. `docs/04-diseno.md` — diseño v1 (con deuda) y diseño v2 (refactorizado), catálogo de smells y refactorings.
5. `docs/05-esquema-backend.md` — entidades, tablas, endpoints, contrato JSON y códigos de error.
6. `docs/06-plan-implementacion.md` — fases, tareas, commits y tags. **Es la fuente de verdad de qué hacer en cada paso.**

## 3. Reglas de trabajo (OBLIGATORIAS)

### 3.1 Trabajar por fases
- Ejecutar **solo la fase que se pida** en el prompt (ver `docs/06-plan-implementacion.md`). No adelantar fases.
- Al terminar una fase: mostrar un resumen de lo hecho, la lista de archivos tocados, los comandos que el autor tiene que correr (indicando si van en **CMD**, **PowerShell** o la terminal de VS Code) y **detenerse**.
- Nunca crear tags de Git sin que el prompt lo pida explícitamente.

### 3.2 El contrato HTTP es sagrado
- Rutas, métodos, nombres de campos JSON, códigos de estado HTTP y reglas de negocio definidos en `docs/05-esquema-backend.md` **deben ser idénticos en v1 y en v2**.
- Los tests de caracterización (`src/test/.../api/`) son la red de seguridad. **No se modifican durante el refactoring.** Si un refactoring rompe un test, se corrige el código de producción, no el test.
- La única diferencia externa permitida entre v1 y v2 es el **formato del cuerpo de error** (texto plano en v1 → JSON `ErrorResponse` en v2). Los tests de caracterización verifican código HTTP y que el mensaje esté contenido en el cuerpo, así que pasan en ambas versiones. Este cambio se documenta como **mejora intencional** en `docs/tpi/refactorings.md`.

### 3.3 Sobre la v1 (deuda técnica intencional)
- La v1 debe **funcionar correctamente** y pasar todos los tests de caracterización.
- La deuda tiene que parecer **la de un proyecto real hecho con apuro**, no una caricatura.
- **PROHIBIDO** dejar en el código de v1 comentarios del tipo `// SMELL`, `// TODO refactor`, `// mal hecho a propósito`. El diagnóstico se documenta en `docs/tpi/`, no en el código.
- Los smells a introducir están listados en `docs/04-diseno.md` §2 (catálogo S01–S12). Introducir **todos**.

### 3.4 Sobre la v2 (refactoring)
- Un refactoring = **un commit**. Antes de cada commit, correr `.\mvnw.cmd test` y comprobar que todo está en verde.
- Cada refactoring se documenta en `docs/tpi/refactorings.md` con: ID, técnica (nombre del catálogo de Fowler), smell que resuelve, principio SOLID / Clean Code, fragmento **antes**, fragmento **después**, archivos afectados, hash del commit y resultado de los tests.
- Usar pasos chicos y seguros: primero mover o extraer, después renombrar, nunca reescribir de cero.

### 3.5 Git
- Mensajes de commit en español, formato Conventional Commits: `feat:`, `test:`, `refactor(R03):`, `docs:`, `chore:`.
- **No agregar líneas `Co-Authored-By` ni firmas de Claude en commits ni PRs.**
- Rama principal: `main`. El refactoring se hace en la rama `refactor/solicitudes` y entra por Pull Request (el PR es parte del code review).

### 3.6 Registro de uso de IA (lo exige la consigna)
- La consigna pide un **registro ordenado de los prompts usados**. Al **inicio** de cada fase, agregar una entrada en `docs/ia/registro-prompts.md` con: número, fecha, fase, prompt recibido (textual), resumen de lo que hizo Claude Code y qué tuvo que revisar o corregir el autor (dejar ese último campo como `_(completar por el autor)_`).
- No inventar contenido en ese registro.

### 3.7 Autoría y defensa
- El autor tiene que poder **defender cada línea** en una defensa oral. Al terminar cada fase, explicar en lenguaje simple **por qué** se hizo cada cambio y qué concepto de la materia aplica (SRP, OCP, DIP, Extract Method, etc.).
- No agregar librerías, patrones ni capas que no estén en `docs/02-TRD.md` o `docs/04-diseno.md`.

### 3.8 Datos
- Solo datos **sintéticos** (nombres, legajos y emails inventados). Nunca datos personales reales.

## 4. Comandos frecuentes (Windows)

Todos se ejecutan en la **terminal integrada de VS Code (PowerShell)**, parado en la raíz del proyecto (donde está `pom.xml`):

| Acción | Comando |
|---|---|
| Compilar y correr tests | `.\mvnw.cmd test` |
| Levantar la app | `.\mvnw.cmd spring-boot:run` |
| Reporte de cobertura (JaCoCo) | `.\mvnw.cmd test jacoco:report` → `target\site\jacoco\index.html` |
| Análisis estático (PMD) | `.\mvnw.cmd pmd:pmd` → `target\site\pmd.html` |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Consola H2 | http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:solicitudes`, user `sa`, sin password) |

En PowerShell usar `curl.exe` (no `curl`, que es un alias de `Invoke-WebRequest`).
