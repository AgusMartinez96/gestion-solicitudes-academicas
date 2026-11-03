# Registro de uso de IA y prompts

> La consigna exige declarar el uso de IA (herramienta, finalidad, grado de intervención) y adjuntar un registro ordenado de los prompts usados.

## Declaración general

| Herramienta | Finalidad | Grado de intervención |
|---|---|---|
| Claude (chat, claude.ai) | Planificación del TPI, armado de la documentación para Claude Code (PRD, TRD, diseño, plan) y de los prompts por fase. | Alto en la planificación; revisada y ajustada por el autor. |
| Claude Code (extensión de VS Code) | Generación de la v1, de los tests de caracterización y de los refactorings; borradores de documentación; revisión asistida del PR. | Alto en la generación de código; **el autor revisó, ejecutó, verificó y comprende cada cambio**. |

Criterio de validación aplicado por el autor: ejecutar los tests después de cada cambio, leer el diff de cada commit, contrastar cada smell y cada refactoring con la bibliografía de la materia y corregir lo que no correspondía.

## Registro cronológico

| # | Fecha | Fase | Herramienta | Prompt (textual) | Qué hizo la IA | Qué revisó / corrigió el autor |
|---|---|---|---|---|---|---|
| 0 | 2026-11-01 | Planificación | Claude (chat) | "Tengo que hacer un trabajo integrador de Metodología con esta consigna…" (+ PDF de consigna y modelo de presentación) | Propuso el caso, el stack y armó el paquete de documentación `CLAUDE.md` + `docs/01..06`. | _(completar por el autor)_ |
| 1 | 2026-11-03 | Fase 0 — Configuración | Claude Code | "Leé CLAUDE.md completo y después todos los archivos de docs/ en el orden que indica CLAUDE.md. Ejecutá SOLO la Fase 0 de docs/06-plan-implementacion.md. Antes de empezar, agregá la entrada correspondiente en docs/ia/registro-prompts.md con este prompt textual. El proyecto es Spring Boot 4.1.2 (NO Boot 3): leé con atención docs/02-TRD.md §1.1. Usá la versión del pom.xml y elegí versiones de springdoc (línea 3.x), jacoco y pmd compatibles con ella y con Java 21 (indicá cuáles elegiste y por qué). Además: start.spring.io generó el paquete ar.edu.utn.tup.gestion_solicitudes_academicas, pero los docs usan ar.edu.utn.tup.solicitudes. Renombrá el paquete (main y test) a ar.edu.utn.tup.solicitudes, la clase principal a SolicitudesApplication y la clase de test a SolicitudesApplicationTests, y borrá las carpetas viejas que queden vacías. Al terminar, corré .\mvnw.cmd test, mostrame el resultado, hacé el commit indicado en el plan, hacé push a origin main y detenete." | Leyó CLAUDE.md y docs/. Agregó springdoc, JaCoCo y PMD al pom.xml, creó config/pmd/ruleset.xml, application.properties, estructura de docs/ y .gitignore; renombró el paquete base y las clases principal y de test; corrió los tests y commiteó. | _(completar por el autor)_ |
