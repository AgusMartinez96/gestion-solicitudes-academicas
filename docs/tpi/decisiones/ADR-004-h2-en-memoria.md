# ADR-004 — H2 en memoria como persistencia

- **Estado:** aceptada
- **Fecha:** 2026-11-04

## Contexto

La consigna admite una "base de datos o memoria estructurada" para el sistema base. El foco del trabajo es la calidad interna y la verificación del refactoring, no la administración de una base de datos. Además, quien corrige tiene que poder ejecutar el proyecto sin instalar nada y obtener siempre los mismos datos.

## Decisión

Se usa **Spring Data JPA con H2 en memoria** (`jdbc:h2:mem:solicitudes;DB_CLOSE_DELAY=-1`), esquema creado al arrancar (`spring.jpa.hibernate.ddl-auto=create-drop`) y datos sintéticos cargados desde `src/main/resources/data.sql` (3 estudiantes, 3 responsables y 5 solicitudes). La consola H2 queda disponible en `/h2-console` (usuario `sa`, sin contraseña, solo para este entorno local).

## Alternativas consideradas

1. **PostgreSQL o MySQL.** Rechazada: exige instalar y configurar un motor, y los resultados dependerían del estado de la base.
2. **Estructuras en memoria sin JPA.** Rechazada: se perdería el mapeo de entidades (con sus relaciones `LAZY`), que forma parte del diseño y de uno de los hallazgos del trabajo (ADR-006).

## Consecuencias

- (+) Cero instalación; el arranque es siempre reproducible y los tests pueden afirmar sobre datos conocidos (por ejemplo, la solicitud 2 con su responsable).
- (+) Los datos son inventados, sin información personal real.
- (−) Los datos no persisten entre ejecuciones.
- (−) H2 no reproduce todas las particularidades de un motor de producción (por ejemplo, el tipo `enum` nativo que Hibernate 7 intentó generar en R02).
- **Límite conocido:** el trabajo no contempla migraciones de esquema ni configuración por entorno.
