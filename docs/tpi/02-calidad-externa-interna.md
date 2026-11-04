# 02 — Calidad externa vs. calidad interna (v1)

> Las referencias `archivo:línea` corresponden al tag `estado-inicial`. `SC` = `SolicitudController`.

La **calidad externa** es lo que percibe quien usa el sistema; la **calidad interna** es lo que ve quien lo mantiene. Se usa el modelo de calidad de producto de **ISO/IEC 25010**.

## 1. Calidad externa

| Característica (ISO/IEC 25010) | Qué se observa en la v1 | Evidencia | Valoración |
|---|---|---|---|
| **Adecuación funcional** (completitud) | Los 8 endpoints del contrato (E1–E8) y las 12 reglas de negocio (RN-01 a RN-12) están implementados. | `docs/pruebas/casos-de-prueba.md` | ✅ |
| **Adecuación funcional** (corrección) | Los códigos HTTP (201, 200, 400, 404, 409) y los mensajes de error coinciden con el contrato. 15 de 15 casos de caracterización pasan. | `docs/evidencias/antes/surefire/` | ✅ |
| **Adecuación funcional** (pertinencia) | El flujo cubre el problema del PRD: registrar, asignar, cambiar de estado, consultar y reportar. | `docs/01-PRD.md` | ✅ |
| **Usabilidad** (capacidad de aprendizaje, operabilidad) | La API se explora y prueba desde Swagger UI sin herramientas extra. Los 8 endpoints están visibles. | `capturas/01-swagger-endpoints.png` | ✅ |
| **Usabilidad** (protección ante errores del usuario) | Los errores de entrada devuelven el código y un mensaje legible. | `capturas/03-error-409.png` | ✅ con reservas: el cuerpo es texto plano y no hay contrato tipado de errores (ver S10 y S05). |
| **Eficiencia / Fiabilidad** | Con H2 en memoria y datos sintéticos, la API responde de inmediato. Arranca con un solo comando y datos de prueba. | `README.md` | ✅ para este alcance |

**Conclusión de calidad externa:** para quien consume la API, la v1 es correcta y completa. Desde afuera no hay motivo para sospechar de la deuda técnica.

## 2. Calidad interna (mantenibilidad)

ISO/IEC 25010 divide la mantenibilidad en cinco subcaracterísticas. Para cada una, los problemas concretos de la v1:

| Subcaracterística | Problema concreto | Smells | Evidencia |
|---|---|---|---|
| **Modularidad** (cambiar un componente sin afectar a otros) | Un único controller concentra validación, reglas, acceso a datos, mapeo a JSON, notificación y reporte. Hay 433 líneas y 7 responsabilidades. Las reglas no están en las entidades. | S01, S08, S11 | `SC` completo; PMD `CyclomaticComplexity` de clase = 119 |
| **Reusabilidad** (usar un activo en varios sistemas) | La lógica de negocio solo existe dentro de métodos de un controller HTTP, atados a `ResponseEntity` y a `Map`. | S01, S06 | `SC:47-123` |
| **Analizabilidad** (diagnosticar causas de fallos) | Métodos con complejidad ciclomática de hasta 32 y NPath de 41.515. El `catch (Exception)` oculta la causa real. Los nombres no revelan la intención. | S02, S09, S10 | PMD: `SC:163` `listar`; 6 `AvoidCatchingGenericException`; 28 `ShortVariable` |
| **Modificabilidad** (modificar sin introducir defectos) | Una regla repetida en varios lugares (el chequeo "cerrada" en 7, el mapeo a JSON en 5 bloques). Agregar un tipo obliga a tocar 6 lugares en 2 archivos (ver §3). | S03, S04, S07 | `SC:104`, `141`, `205`, `265`, `323`, `294`, `375` |
| **Capacidad de ser probado** (facilidad de verificar) | No se puede probar la lógica sin levantar Spring y sin pasar por HTTP. El reloj (`LocalDateTime.now()`) y el notificador (`new NotificadorConsola()`) no se pueden sustituir. | S11, S01 | `SC:75`, `249`, `252`; no hay tests unitarios posibles sobre las reglas |

### Métricas de la v1

| Métrica | Valor | Fuente |
|---|---|---|
| Tests de caracterización | 15 / 15 en verde (16 con `contextLoads`) | `docs/evidencias/antes/surefire/` |
| Cobertura JaCoCo | 82 % de instrucciones, 60 % de ramas | `docs/evidencias/antes/jacoco/index.html` |
| Violaciones PMD | 56 | `docs/evidencias/antes/pmd/pmd.xml` |
| Líneas de `SolicitudController` | 433 | `SC` |
| Complejidad ciclomática de `SC` | 119 en total (método más complejo: 32) | PMD `CyclomaticComplexity` (`SC:35`) |
| Método más largo (NCSS) | 55 (`crear`) | PMD `NcssCount` (`SC:47`) |
| Tipos de producción | 11 (8 clases y 3 interfaces de repositorio) | `src/main/java` |

La cobertura de 82 % es alta pero engañosa: mide que el código se ejecuta, no que esté bien diseñado. Los tests de caracterización dan la red de seguridad, no calidad interna.

## 3. Conclusión: "funciona, pero es caro de cambiar"

La v1 cumple todo lo que el usuario puede ver, pero cada cambio cuesta más de lo que debería porque las reglas están repetidas y dispersas. Para mostrarlo con un caso concreto:

### Ejemplo de costo del cambio: agregar el tipo `BECA`

Supongamos que se pide un tipo nuevo `BECA` con plazo de 10 días y prioridad por defecto `MEDIA`. En la v1 hay que tocar **6 lugares en 2 archivos**, y el compilador no avisa de ninguno:

| # | Archivo:líneas | Qué hay que modificar | Qué pasa si se olvida |
|---|---|---|---|
| 1 | `util/Utils.java:7-22` | Agregar `case "BECA": return 10;` en `calcularDias`. | Cae en `default: return 7`: la solicitud se crearía con un plazo equivocado, **sin ningún error**. |
| 2 | `util/Utils.java:32-33` | Agregar `tipo.equals("BECA")` a la cadena de validación de `validar`. | Esa validación rechazaría `BECA` con 400 `Tipo de solicitud inválido`. |
| 3 | `controller/SolicitudController.java:57-58` | Agregar `tipo.equals("BECA")` a la validación de `crear`. | `crear` rechazaría `BECA` con 400 antes de llegar a nada. |
| 4 | `controller/SolicitudController.java:61-69` | Agregar la prioridad por defecto en el `if/else` (`MEDIA`). | Cae en el `else` final: prioridad `BAJA`, **sin error**. |
| 5 | `controller/SolicitudController.java:172-173` | Agregar `tipo.equals("BECA")` al filtro de `listar`. | `GET ?tipo=BECA` respondería 400. |
| 6 | `controller/SolicitudController.java:362-367` | Agregar `porTipo.put("BECA", 0)` en `reporte`. | Por lectura del código, `porTipo.get("BECA")` es `null` y `+ 1` lanza `NullPointerException`; el `catch (Exception)` de `SC:384` lo convierte en un 500 con cuerpo `Error: null`. |

Lo grave no es la cantidad de lugares, sino que **dos de los seis fallan en silencio** (plazo y prioridad equivocados) y otro **falla lejos de donde se originó el cambio** (el reporte). Es el síntoma de S04 y S07: el conocimiento "qué tipos existen" está copiado como texto en varios sitios.

En la v2 el mismo cambio será **agregar una constante al enum `TipoSolicitud`** con su plazo y su prioridad por defecto (ver `docs/04-diseno.md` §4, R02 y R04).

### Veredicto

| | Calidad externa | Calidad interna |
|---|---|---|
| v1 | Alta: completa y correcta | Baja: 12 smells, 56 violaciones PMD, cambios costosos y riesgosos |

El detalle de cada smell está en `docs/tpi/03-deuda-tecnica-code-smells.md`.
