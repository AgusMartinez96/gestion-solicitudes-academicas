# Anexo: contenido exacto del ZIP

Este archivo lista **qué meter en el ZIP de anexos y en qué carpetas**. El informe (PDF o documento) y el video **no** van en el ZIP: el informe se entrega aparte y el video se enlaza desde el informe (`{{URL_VIDEO}}`).

Nombre sugerido: `Martinez_Agustin_TPI_MetodologiaII_Anexos.zip`

## Estructura del ZIP

```
Anexos/
├── 00-LEEME.md
├── 01-reportes-v1/
├── 02-reportes-v2/
├── 03-capturas/
│   ├── antes/
│   └── despues/
├── 04-diagnostico/
├── 05-refactoring-y-diseno/
│   └── decisiones/
├── 06-pruebas/
├── 07-code-review/
├── 08-uso-de-ia/
└── 09-codigo-fuente/
```

## Qué va en cada carpeta

| Carpeta del ZIP | Archivos de origen en el repositorio | Para qué sirve |
|---|---|---|
| `00-LEEME.md` | `README.md` (copia de la raíz) | Qué es el proyecto, cómo ejecutarlo y qué tag es cada versión |
| `01-reportes-v1/` | `docs/evidencias/antes/surefire/`, `docs/evidencias/antes/jacoco/`, `docs/evidencias/antes/pmd/` | Evidencia cuantitativa de la v1 (16 tests, 82 % / 60 % de cobertura, 56 violaciones PMD) |
| `02-reportes-v2/` | `docs/evidencias/despues/surefire/`, `docs/evidencias/despues/jacoco/`, `docs/evidencias/despues/pmd/` | Evidencia cuantitativa de la v2 (105 tests, 95,5 % / 86,1 %, 9 violaciones PMD) |
| `03-capturas/antes/` | `docs/evidencias/antes/capturas/` (6 imágenes: `01-swagger-endpoints`, `02-post-201`, `03-error-409`, `04-reporte`, `05-notificacion-consola`, `06-example-value-vacio`) | Capturas de la v1 citadas en el informe |
| `03-capturas/despues/` | `docs/evidencias/despues/capturas/` (7 imágenes: `01-swagger-endpoints`, `02-tests-intactos`, `03-error-409-json`, `06-example-value-tipado`, `07-pull-request-review`, `08-decisiones-review`, `09-pr-merged`) | Capturas de la v2 y del Pull Request citadas en el informe |
| `04-diagnostico/` | `docs/tpi/01-sistema-base.md`, `docs/tpi/02-calidad-externa-interna.md`, `docs/tpi/03-deuda-tecnica-code-smells.md` | Diagnóstico completo con evidencia `archivo:línea` |
| `05-refactoring-y-diseno/` | `docs/tpi/refactorings.md`, `docs/tpi/04-solid-clean-code.md`, `docs/tpi/08-conclusiones.md`, `docs/04-diseno.md` | Fichas de los 9 refactorings, SOLID y Clean Code con diagramas, conclusiones |
| `05-refactoring-y-diseno/decisiones/` | `docs/tpi/decisiones/ADR-001-enum-con-comportamiento.md`, `ADR-002-filtrado-en-memoria.md`, `ADR-003-cuerpo-de-error-json.md`, `ADR-004-h2-en-memoria.md`, `ADR-005-validacion-manual.md`, `ADR-006-entitygraph-y-tests-no-transaccionales.md` | Decisiones técnicas |
| `06-pruebas/` | `docs/tpi/06-pruebas-verificacion.md`, `docs/pruebas/casos-de-prueba.md` | Estrategia, CP-01..CP-15 en v1 y v2 y métricas |
| `07-code-review/` | `docs/tpi/07-code-review-checklist.md`, `docs/tpi/pr-descripcion.md` | Checklist, 14 observaciones, decisiones y descripción del PR #1 |
| `08-uso-de-ia/` | `docs/ia/registro-prompts.md` | Registro ordenado de los prompts (exigido por la consigna) |
| `09-codigo-fuente/` | `codigo-v1-estado-inicial.zip` y `codigo-v2-estado-refactorizado.zip` (se generan con los comandos de abajo) | Código fuente de cada versión, sin `.git` ni `target/` |

## Cómo generar los dos ZIP de código

Desde la raíz del repositorio, en PowerShell, con el árbol limpio (`git status`). `git archive` exporta el contenido exacto de cada tag, sin `.git` ni `target\`:

```powershell
New-Item -ItemType Directory -Force -Path .\Anexos\09-codigo-fuente | Out-Null
git archive --format=zip -o .\Anexos\09-codigo-fuente\codigo-v1-estado-inicial.zip estado-inicial
git archive --format=zip -o .\Anexos\09-codigo-fuente\codigo-v2-estado-refactorizado.zip estado-refactorizado
```

Después copiar a `Anexos\` las carpetas y archivos de la tabla anterior y comprimir:

```powershell
Compress-Archive -Path .\Anexos\* -DestinationPath .\Martinez_Agustin_TPI_MetodologiaII_Anexos.zip
```

> Ejecutá esto **después** de que existan los tags `estado-refactorizado` y `etapa-docs` (se crean al cerrar la Fase 7). La carpeta `Anexos\` y el ZIP son artefactos de entrega: no los agregues al repositorio.

## Qué NO meter

- `target/`, `.git/`, `.idea/`, `.vscode/` ni el `mvnw` descargado.
- Datos personales reales: el proyecto solo usa datos sintéticos.
- Los archivos de la guía de trabajo (`CLAUDE.md`, `docs/06-plan-implementacion.md`): son internos del proceso, no un entregable.
- El borrador del informe (`docs/tpi/informe-borrador.md`) y el guion del video (`docs/tpi/guion-video.md`): sirven para preparar la entrega, no van como anexo.

## Verificación final del ZIP

- [ ] Se abre y tiene las 10 entradas de la estructura (`00-LEEME.md` y las carpetas `01` a `09`).
- [ ] Las 13 capturas están (6 de `antes`, 7 de `despues`).
- [ ] Los dos ZIP de código descomprimen y compilan: `.\mvnw.cmd test` → 16 tests en v1 y 105 en v2.
- [ ] Todos los nombres y referencias del informe (§12 Anexos) coinciden con las carpetas.
