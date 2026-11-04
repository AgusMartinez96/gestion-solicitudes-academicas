# Guion del video (12 minutos)

> **Cómo usarlo:** son viñetas para hablar con voz propia, no un texto para leer. Ensayá cada bloque una vez con el cronómetro y ajustá. Las cifras de este guion están verificadas en los documentos de `docs/tpi/`. Antes de grabar, pegá en `{{URL_VIDEO}}` del informe el enlace del video ya subido.

## Resumen de tiempos

| # | Bloque | Duración | Acumulado |
|---|---|---|---|
| 1 | Presentación (con cámara) | 0:45 | 0:45 |
| 2 | Sistema base | 1:30 | 2:15 |
| 3 | Diagnóstico: calidad externa vs. interna | 1:15 | 3:30 |
| 4 | Code smells y deuda técnica | 1:30 | 5:00 |
| 5 | SOLID y refactoring | 2:30 | 7:30 |
| 6 | Pruebas (con demo en vivo) | 2:00 | 9:30 |
| 7 | Code review | 1:30 | 11:00 |
| 8 | Conclusiones | 1:00 | 12:00 |

## Preparación antes de grabar (PowerShell)

Dos versiones corriendo a la vez evitan esperas en cámara:

```powershell
# Terminal 1: v2 en el puerto 8080 (carpeta del proyecto, rama main)
cd C:\Users\Agustin\Desktop\Metodologia\gestion-solicitudes-academicas
.\mvnw.cmd spring-boot:run

# Terminal 2: v1 en el puerto 8081, en una copia aparte del tag estado-inicial
git worktree add ..\gsa-v1 estado-inicial
cd ..\gsa-v1
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

- Swagger v2: http://localhost:8080/swagger-ui.html · Swagger v1: http://localhost:8081/swagger-ui.html
- Probalo todo antes: si algún comando no anda, usá las capturas de `docs/evidencias/` como respaldo.
- Terminal 3 libre para la demo del `@EntityGraph` (bloque 6). Al terminar: `git worktree remove ..\gsa-v1`.
- Tené abiertas en pestañas: `docs/tpi/03-deuda-tecnica-code-smells.md`, `docs/tpi/refactorings.md`, el PR #1 y `docs/evidencias/`.

---

## 1. Presentación — 0:45 (con cámara)

**En pantalla:** tu cara; después una diapositiva o el README con el título.

**Decir (viñetas):**
- Nombre completo: Agustín Martínez. Metodología de Sistemas II, Tecnicatura en Programación a Distancia, UTN. Comisión 5, docente Diego Lobos. Entrega individual.
- El trabajo no trata de hacer un sistema grande, sino de **mejorar uno existente sin cambiar lo que ve el usuario**.
- Voy a mostrar: el sistema, su diagnóstico, el refactoring, cómo comprobé que no rompí nada, y el code review.
- Usé IA (Claude y Claude Code); está declarado y los prompts están registrados. Yo revisé y decidí.

## 2. Sistema base — 1:30

**En pantalla:** Swagger de la v2 (`http://localhost:8080/swagger-ui.html`). Alternativa: `docs/evidencias/antes/capturas/01-swagger-endpoints.png`.

**Decir:**
- Problema: trámites académicos por mail y ventanilla, sin trazabilidad. El sistema los centraliza.
- API REST en Java 21 y Spring Boot 4.1.2, H2 en memoria, datos inventados.
- 7 funcionalidades y 8 endpoints: registrar, consultar, buscar, cambiar estado, asignar responsable, reporte, listados.
- Mostrar en Swagger: `POST /api/solicitudes` con un ejemplo → 201; después `GET /api/solicitudes/2` con estudiante y responsable.
- Reglas clave: máquina de estados (pendiente → en análisis → resuelta/rechazada) y plazo según tipo.
- Dos versiones en el mismo repo, separadas por tags: `estado-inicial` (v1) y `estado-refactorizado` (v2). **Mismo contrato HTTP.**

## 3. Diagnóstico: calidad externa vs. interna — 1:15

**En pantalla:** `docs/tpi/02-calidad-externa-interna.md` (tabla de calidad interna y ejemplo de `BECA`).

**Decir:**
- ISO/IEC 25010: calidad externa es lo que percibe el usuario; interna es lo que sufre quien mantiene.
- v1 por fuera: completa y correcta, 15 de 15 casos pasan. Por dentro: baja.
- Números de la v1: controller de **433 líneas**, **56 violaciones de PMD**, método más largo de **77 líneas**.
- Ejemplo que lo explica solo: **agregar el tipo BECA** → 6 lugares en 2 archivos y **2 fallan en silencio**. En la v2: una constante en un enum.
- Cobertura de 82 % engaña: mide que se ejecuta, no que esté bien diseñado.

## 4. Code smells y deuda técnica — 1:30

**En pantalla:** tabla de los 12 smells (`03-deuda-tecnica-code-smells.md` §3); después el código real de la v1: `git show estado-inicial:src/main/java/ar/edu/utn/tup/solicitudes/controller/SolicitudController.java` (mostrar `crear` y el bloque de mapeo copiado). Captura de respaldo: `docs/evidencias/antes/capturas/03-error-409.png`.

**Decir:**
- Encontré 12 smells; los más graves: God Class, método largo, código duplicado, obsesión con primitivos y manejo de errores.
- Mostrar: el mismo bloque de mapeo copiado 5 veces; el `catch (Exception)` y el `"404|..."` dentro del texto.
- Deuda según Fowler: imprudente y deliberada, y lo aclaro: **la construí a propósito** para poder diagnosticarla.
- Hallazgo honesto: **PMD no marcó la God Class** (la regla mira cohesión TCC y quedó apenas sobre el umbral). Y **6 de los 12 smells ningún analizador los ve**: hay que leer el diseño.

## 5. SOLID y refactoring — 2:30

**En pantalla:** `docs/tpi/refactorings.md` (tabla de los 9) y luego el código de la v2: `dominio/EstadoSolicitud.java`, `dominio/TipoSolicitud.java`, `aplicacion/SolicitudService.java`. Diagramas en `docs/tpi/04-solid-clean-code.md`.

**Decir:**
- Antes de tocar nada escribí **15 tests de caracterización** sobre la v1. Son mi red de seguridad y **no los modifiqué**.
- Nueve refactorings, un commit cada uno, con los tests en verde en cada paso.
- **R01 Extract Method:** `crear` pasó de 77 líneas a delegar en métodos chicos.
- **R03 Move Method:** la regla de transiciones salió del controller y vive en `EstadoSolicitud.puedeTransicionarA` (mostrar el `switch`). La entidad ahora protege sus reglas.
- **R05 Introduce Parameter Object:** `armarSolicitud` de 7 parámetros a 3; `Map<String,Object>` a records.
- SOLID: **SRP** (controller, service, mapper, manejador de errores), **OCP** (nuevo tipo = una constante), **DIP** (`SolicitudService` depende de `Notificador` y `Clock`).
- Honestidad: LSP e ISP se cumplen pero con poca superficie; no inventé abstracciones.
- Resultado: controller de 433 a 100 líneas (unas 70 de lógica), PMD de 56 a 9, método más largo de 77 a 15.

## 6. Pruebas — 2:00 (demo en vivo)

**En pantalla:** `docs/tpi/06-pruebas-verificacion.md` (tabla de tests y de métricas) y la terminal.

**Decir (primera parte, 0:40):**
- 105 tests: 15 de caracterización, 63 de dominio, 11 del service con reloj fijo y notificador falso, 4 del manejador de errores, 11 de integración y 1 de contexto.
- Los mismos 15 casos pasan en v1 y en v2. Cobertura de instrucciones de 82,2 % a 95,5 % (medida con `clean`; mi primera medición sin `clean` salió inflada).

**Demo A — el experimento del `@EntityGraph` (1:00):**

1. Contar el problema: los tests de caracterización son transaccionales y **no ven** un `LazyInitializationException`; me pasó en R06.
2. Abrir `src/main/java/ar/edu/utn/tup/solicitudes/persistencia/SolicitudRepository.java` y **borrar las dos líneas `@EntityGraph(...)`**.
3. Correr los dos tests, uno por vez (o dejar el primero ya corrido para ahorrar tiempo):
   ```powershell
   .\mvnw.cmd test -Dtest=SolicitudesApiCaracterizacionTest      # sigue 15/15 en verde
   .\mvnw.cmd test -Dtest=IntegracionHttpNoTransaccionalTest      # falla: LazyInitializationException (4 de 11)
   ```
4. Mostrar el mensaje `Could not initialize proxy [...Responsable#1] - no session`.
5. **Restaurar sin commitear:**
   ```powershell
   git checkout -- src/main/java/ar/edu/utn/tup/solicitudes/persistencia/SolicitudRepository.java
   git status
   ```
- Moraleja: un test que no puede fallar no protege; por eso agregué el no transaccional (ADR-006).

**Demo B — antes/después de Swagger (en el bloque 5 o acá, 0:20):**
- Pestaña v1 (`localhost:8081`): `GET /api/reportes/resumen` está dentro de `solicitud-controller` y el "Example Value" sale `{}`.
- Pestaña v2 (`localhost:8080`): `reporte-controller` separado y esquemas tipados con errores 400/404/409 documentados.
- Respaldo: `docs/evidencias/antes/capturas/06-example-value-vacio.png` y `docs/evidencias/despues/capturas/06-example-value-tipado.png`.
- Mostrar un error 409 en la v1 (texto plano) y en la v2 (JSON): `docs/evidencias/antes/capturas/03-error-409.png` y `despues/capturas/03-error-409-json.png`. **Es el único cambio externo intencional.**

## 7. Code review — 1:30

**En pantalla:** PR #1 en GitHub (`docs/evidencias/despues/capturas/07-pull-request-review.png`, `08-decisiones-review.png` y `09-pr-merged.png`) y `docs/tpi/07-code-review-checklist.md`.

**Decir:**
- Abrí el PR #1 y lo revisé con un checklist de siete áreas. La revisión fue asistida por IA, pero **las decisiones fueron mías**.
- 14 observaciones: 3 mayores, 8 menores, 3 sugerencias. **11 aceptadas y corregidas, 1 diferida, 2 rechazadas.**
- Ejemplo de aceptada (O-1): la asignación de responsable validaba en el service y no en la entidad; la moví al dominio **respetando el orden de errores del contrato**.
- Ejemplo de decisión técnica (O-3): el TRD pedía Bean Validation; no garantizaba el orden ni los mensajes del contrato y la quité con un ADR.
- Ejemplos de rechazadas: O-12 (los 4 parámetros del listado son el contrato HTTP) y O-13 (arreglar los espacios en blanco cambiaría el comportamiento heredado, y el TPI exige preservarlo). Se registran como límite conocido.
- Después de cada corrección: tests en verde y una batería de 35 llamadas HTTP sin diferencias.
- Merge con *merge commit*, para conservar un commit por refactoring.

## 8. Conclusiones — 1:00 (con cámara)

**En pantalla:** tu cara, o la tabla de resultado de `docs/tpi/08-conclusiones.md` §1.

**Decir (con tus palabras):**
- Lo que cambió: el sistema hace lo mismo por fuera, pero ahora es barato de probar y de modificar.
- Aprendizajes reales: pasos chicos con red de seguridad; las herramientas no reemplazan la lectura del diseño; una métrica solo vale si sabés cómo la mediste.
- Hallazgos: columnas enum de Hibernate 7, el `LazyInitializationException` que los tests no veían, la cobertura inflada sin `clean`.
- Límites: sin autenticación, H2 en memoria, filtrado en memoria. Mejora futura: filtros y paginación en la base, roles, persistencia real.
- _(Cierre personal: qué te llevás de este trabajo y cómo usarías esto en tu trabajo real.)_
- Agradecer y cerrar: repositorio y tags en el informe.

---

## Checklist de grabación

- [ ] Cámara encendida en la presentación y en las conclusiones; nombre completo dicho en voz alta.
- [ ] Cronometrado: 12:00 como máximo.
- [ ] `git status` limpio al final (el `@EntityGraph` restaurado y sin commitear).
- [ ] Los dos Swagger probados antes de grabar.
- [ ] Sin datos personales reales a la vista.
- [ ] Enlace del video pegado en `{{URL_VIDEO}}` del informe.
