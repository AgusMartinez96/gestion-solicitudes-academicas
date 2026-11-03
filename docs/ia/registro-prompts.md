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
