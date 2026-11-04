# 03 — Deuda técnica y code smells (v1)

> **Alcance del diagnóstico:** tag `estado-inicial` (commit `17fb47f`). Todos los fragmentos son código real de la v1 y las referencias `archivo:línea` corresponden a ese tag.
> **Abreviaturas:** `SC` = `controller/SolicitudController.java`; `SoC` = `controller/SoporteController.java`.
> **Evidencia de PMD:** `docs/evidencias/antes/pmd/pmd.xml` (y `pmd.html`). Reglas usadas: `config/pmd/ruleset.xml`.

## 1. Resumen ejecutivo

La v1 funciona: 15 de 15 casos de caracterización pasan, con 82 % de cobertura de instrucciones y 60 % de ramas (JaCoCo). Aun así, concentra 12 smells (S01 a S12) y PMD informa **56 violaciones**. PMD detecta 9 de las 11 reglas configuradas con hallazgos, pero solo cubre síntomas de tamaño, complejidad, nombres y manejo de errores; no ve la duplicación, los strings mágicos de dominio, el modelo anémico ni el acoplamiento. Esas partes del diagnóstico se apoyan en lectura del código.

### 1.1 Violaciones PMD por regla (total: 56)

| Regla PMD | Violaciones | Smells con los que se relaciona |
|---|---|---|
| `ShortVariable` | 28 | S09 |
| `CyclomaticComplexity` | 6 | S01, S02 |
| `AvoidCatchingGenericException` | 6 | S10 |
| `NcssCount` | 5 | S01, S02 |
| `SystemPrintln` | 3 | S12 |
| `NPathComplexity` | 3 | S02 |
| `CognitiveComplexity` | 3 | S02 |
| `ExcessiveParameterList` | 1 | S05 |
| `AvoidLiteralsInIfCondition` | 1 | S09 |
| `GodClass` | 0 | S01 (ver §2, no dispara) |
| `AvoidReassigningParameters` | 0 | — |
| **Total** | **56** | |

Por archivo: `SolicitudController` 46 (incluye 1 de `CyclomaticComplexity` a nivel de clase), `SoporteController` 2, `Estudiante` 2, `Responsable` 2, `Solicitud` 2, `NotificadorConsola` 1, `Utils` 1. Total: 56.

---

## 2. Fichas de smells

### S01 — God Class / Large Class

- **Evidencia (`SC:33-47`):**
  ```java
  @RestController
  @Transactional
  public class SolicitudController {

      @Autowired
      private SolicitudRepository solicitudRepository;

      @Autowired
      private EstudianteRepository estudianteRepository;

      @Autowired
      private ResponsableRepository responsableRepository;

      @PostMapping("/api/solicitudes")
      public ResponseEntity<Object> crear(@RequestBody Map<String, Object> body) {
  ```
- **Ubicación:** `SC:35` (clase completa, 433 líneas).
- **Por qué es una God Class:** una sola clase resuelve, en métodos propios, **siete responsabilidades** distintas:

  | # | Responsabilidad | Dónde |
  |---|---|---|
  | 1 | Recibir y validar la entrada (casteos de `Map`, validaciones) | `SC:47-74`, `168-179`, `236-240` |
  | 2 | Reglas de negocio (prioridad por defecto, plazo, transiciones, cierre) | `SC:61-91`, `241-247`, `389-401` |
  | 3 | Acceso a datos (3 repositorios) | `SC:37-44` y cada endpoint |
  | 4 | Armado de la respuesta JSON a mano | `SC:95-118`, `132-155`, `196-219`, `256-279`, `314-337` |
  | 5 | Notificación al estudiante | `SC:251-254` |
  | 6 | Reporte estadístico | `SC:344-387` |
  | 7 | Traducción de errores a HTTP | `SC:417-432` |

- **Evidencia visual:** en `docs/evidencias/antes/capturas/01-swagger-endpoints.png`, Swagger UI agrupa por controller. Dentro de `solicitud-controller` conviven `GET/POST /api/solicitudes`, los dos `PATCH`, `GET /api/solicitudes/{id}` **y** `GET /api/reportes/resumen`. Un reporte no es una solicitud: está ahí porque el controller se quedó con todo.
- **Métricas:**

  | Métrica | Valor | Fuente |
  |---|---|---|
  | Líneas | 433 (la siguiente clase más grande, `Solicitud`, tiene 132 y es solo accesores) | `wc -l` |
  | Endpoints públicos + métodos privados | 6 + 3 | `SC:47, 126, 163, 229, 287, 345` / `389, 403, 417` |
  | Complejidad ciclomática de la clase | **119** (método más complejo: 32) | PMD `CyclomaticComplexity`, `SC:35` |
  | Métodos con NCSS ≥ 30 | 5 (`crear` 55, `listar` 44, `asignarResponsable` 43, `cambiarEstado` 41, `reporte` 31) | PMD `NcssCount` |
  | Dependencias inyectadas | 3 repositorios + `Utils` estático + `NotificadorConsola` instanciado | `SC:37-44`, `252` |

- **Impacto:** cualquier cambio (regla, formato JSON, reporte, notificación) toca el mismo archivo; riesgo de conflictos de merge y de efectos colaterales; es imposible probar una regla sin pasar por HTTP.
- **Riesgo de mantenimiento:** Alto.
- **Prioridad:** Alta.
- **Refactoring propuesto:** **R06** (Extract Class: `SolicitudService`, `ReporteService`, `SolicitudMapper`, `ReporteController`), con apoyo de R03 (reglas al dominio).
- **Detectado por PMD:** **No por `GodClass`** (0 violaciones). Sí aparecen síntomas en `CyclomaticComplexity` (clase = 119) y `NcssCount`.

#### Por qué la regla `GodClass` de PMD no lo detecta

La regla de PMD implementa la estrategia de detección de Lanza y Marinescu (*Object-Oriented Metrics in Practice*) y exige que se cumplan **las tres condiciones a la vez**. Los umbrales los confirmé leyendo el bytecode de `GodClassRule` en `pmd-java-7.17.0.jar`:

| Métrica | Qué mide | Umbral para marcar God Class | Valor en `SolicitudController` | ¿Cumple? |
|---|---|---|---|---|
| **WMC** (Weighted Method Count) | Suma de la complejidad ciclomática de los métodos | ≥ 47 | ≥ 100 (la regla `CyclomaticComplexity` reporta 119) | Sí |
| **ATFD** (Access To Foreign Data) | Cuántos datos de otras clases usa | > 5 | > 5 | Sí |
| **TCC** (Tight Class Cohesion) | Fracción de pares de métodos que comparten el uso de atributos de la clase | < 1/3 (≈ 0,333) | entre 0,333 y 0,5 | **No** |

Los valores de ATFD y TCC no los imprime PMD cuando la regla no dispara; los medí con reglas XPath temporales (`pmd-java:metric('ATFD')`, `metric('TCC')`) ejecutadas fuera del repositorio sobre el mismo código. El resultado: el controller supera por mucho el umbral de complejidad (WMC) y usa muchos datos ajenos (ATFD), pero su TCC queda **apenas por encima** de 1/3, y por eso la regla no salta.

La lectura honesta es que **la heurística no falla: mide otra cosa**. TCC mide cohesión por uso compartido de los atributos de la clase; no mide cuántas responsabilidades de negocio mezcla. Un valor apenas superior al umbral no dice que el controller sea cohesivo en términos de diseño: dice que, según esa fórmula, no alcanza el corte. (No analicé el cálculo interno de TCC método por método; me apoyo en el valor medido y en la definición de la regla.) Por eso el diagnóstico de S01 se sostiene con métricas de tamaño y complejidad (433 líneas, WMC 119, 5 métodos con NCSS ≥ 30), con el conteo de responsabilidades de la tabla de arriba y con la captura de Swagger, y no con la regla `GodClass`. En el informe de después se podrá mostrar que, tras R06, la complejidad por clase baja aunque `GodClass` siguiera sin disparar.

---

### S02 — Long Method

- **Evidencia (`SC:180-192`, la búsqueda con filtros de `listar`):**
  ```java
  List<Solicitud> lista = solicitudRepository.findAll();
  List<Solicitud> lista2 = new ArrayList<>();
  for (Solicitud s : lista) {
      if (estado == null || s.getEstado().equals(estado)) {
          if (tipo == null || s.getTipo().equals(tipo)) {
              if (prioridad == null || s.getPrioridad().equals(prioridad)) {
                  if (legajo == null || s.getEstudiante().getLegajo().equals(legajo)) {
                      lista2.add(s);
                  }
              }
          }
      }
  }
  ```
- **Ubicación:** `SC:47` (`crear`, 77 líneas), `SC:163` (`listar`, 64 líneas), `SC:345` (`reporte`, 43 líneas), además de `cambiarEstado` (`SC:229`) y `asignarResponsable` (`SC:287`).
- **Impacto:** cada método mezcla niveles de abstracción (leer el `Map`, validar, aplicar reglas, persistir, armar JSON); hay que leerlo entero para saber qué hace; no se puede probar una parte sin ejecutar todo.
- **Riesgo de mantenimiento:** Alto. **Prioridad:** Alta.
- **Refactoring propuesto:** **R01** (Extract Method), luego R06.
- **Detectado por PMD (16 violaciones: 15 en `SC` y 1 en `Utils`):**

  | Método | NcssCount | CyclomaticComplexity | CognitiveComplexity | NPath |
  |---|---|---|---|---|
  | `crear` (`SC:47`) | 55 | 22 | 20 | 630 |
  | `listar` (`SC:163`) | 44 | 32 | 35 | 41.515 |
  | `cambiarEstado` (`SC:229`) | 41 | 20 | 15 | 378 |
  | `asignarResponsable` (`SC:287`) | 43 | 16 | — | — |
  | `reporte` (`SC:345`) | 31 | — | — | — |
  | `Utils.validar` (`Utils.java:28`) | — | 19 | — | — |

  Umbrales: NcssCount ≥ 30 (configurado en el ruleset), NPath > 200 y CognitiveComplexity ≥ 15 (valores por defecto de PMD). El `listar` es el peor caso: 41.515 caminos de ejecución posibles por sus cuatro `if` anidados.

---

### S03 — Duplicated Code

- **Evidencia 1 — el mismo bloque de mapeo a JSON, copiado 5 veces (`SC:95-118`; idéntico en `132-155`, `196-219` con `aux` en lugar de `res`, `256-279` y `314-337`):**
  ```java
  Map<String, Object> res = new LinkedHashMap<>();
  res.put("id", s.getId());
  res.put("tipo", s.getTipo());
  res.put("descripcion", s.getDescripcion());
  res.put("prioridad", s.getPrioridad());
  res.put("estado", s.getEstado());
  res.put("fechaCreacion", s.getFechaCreacion());
  res.put("fechaVencimiento", s.getFechaVencimiento());
  res.put("fechaActualizacion", s.getFechaActualizacion());
  res.put("vencida", !(s.getEstado().equals("RESUELTA") || s.getEstado().equals("RECHAZADA")
          || s.getEstado().equals("CANCELADA")) && s.getFechaVencimiento().isBefore(LocalDateTime.now()));
  ```
- **Evidencia 2 — la condición "solicitud cerrada", copiada 7 veces.** La condición repetida es siempre la misma:
  ```java
  s.getEstado().equals("RESUELTA") || s.getEstado().equals("RECHAZADA")
          || s.getEstado().equals("CANCELADA")
  ```
  Lo que cambia es el contexto en el que aparece cada copia:

  | Copias | Ubicación | Contexto |
  |---|---|---|
  | 5 | `SC:104-105`, `141-142`, `205-206`, `265-266`, `323-324` | Dentro del cálculo de `vencida` del bloque de mapeo (S03, evidencia 1), negada: `!(...) && s.getFechaVencimiento().isBefore(LocalDateTime.now())`. |
  | 1 | `SC:294-295` | En `asignarResponsable`, como `if (...)` que lanza `new RuntimeException("409|La solicitud está cerrada: " + id)` (`SC:296`). Es la única copia con `throw`. |
  | 1 | `SC:375-376` | En `reporte`, como `if (!(...))` para contar las vencidas, sin `throw`. |
- **Evidencia 3 — mapeo duplicado en `SoC`:** `SoC:29-35` (estudiantes) y `SoC:43-49` (responsables) repiten el patrón `Map` + `put` campo por campo.
- **Impacto:** un campo nuevo en el JSON obliga a editar 5 bloques; un estado cerrado nuevo, 7 condiciones. Olvidar uno produce respuestas inconsistentes entre endpoints.
- **Riesgo de mantenimiento:** Alto. **Prioridad:** Alta.
- **Refactoring propuesto:** **R01** (Extract Method: `aMap(Solicitud)` y `estaCerrada(Solicitud)`), después R03 y R06 (`SolicitudMapper`).
- **Detectado por PMD:** No. Las reglas configuradas no incluyen detección de duplicados (la herramienta de PMD para eso es CPD, que no se usó).

---

### S04 — Primitive Obsession / Magic Strings

- **Evidencia (`Solicitud.java:22-31`):**
  ```java
  private String tipo;
  ...
  private String prioridad;
  ...
  private String estado;
  ```
  y comparaciones por texto como `SC:57-58`:
  ```java
  if (tipo == null || !(tipo.equals("CERTIFICADO_ALUMNO_REGULAR") || tipo.equals("REVISION_EXAMEN")
          || tipo.equals("EQUIVALENCIA") || tipo.equals("CAMBIO_COMISION") || tipo.equals("OTRO"))) {
  ```
- **Ubicación:** `Solicitud.java:22, 28, 31`; literales en `SC` y `Utils`.
- **Medición:** en `controller/` y `util/` hay **49 literales de estado**, **30 de tipo** y **11 de prioridad** escritos a mano.
- **Impacto:** un error de tipeo (`"RESUELTA "`, `"CANCELADO"`) compila y falla en ejecución sin avisar; el compilador no ayuda; el conocimiento de "qué valores existen" está copiado en decenas de sitios.
- **Riesgo de mantenimiento:** Alto. **Prioridad:** Alta.
- **Refactoring propuesto:** **R02** (Replace Type Code with Enum).
- **Detectado por PMD:** No. `AvoidLiteralsInIfCondition` solo marcó un literal numérico (S09) y no revisa strings.

---

### S05 — Long Parameter List + Data Clumps (+ falta de contrato tipado)

- **Evidencia 1 (`SC:403-404`):**
  ```java
  private Solicitud armarSolicitud(String legajo, String tipo, String descripcion, String prioridad,
          LocalDateTime creacion, int dias, Estudiante est) {
  ```
  Siete parámetros, cuatro de ellos `String` consecutivos (fácil de intercambiar sin que el compilador lo note).
- **Evidencia 2 — data clump:** `Utils.validar(String descripcion, String tipo, String prioridad, String legajo)` (`Utils.java:28`) recibe los mismos datos que viajan juntos por todo `crear` (llamada en `SC:70`).
- **Evidencia 3 — entrada y salida sin tipo:** los endpoints reciben `@RequestBody Map<String, Object> body` (`SC:47`, `230`, `288`) y devuelven `ResponseEntity<Object>` con `Map<String, Object>`. Consecuencia visible en Swagger: como springdoc no puede inferir la forma del `Map<String, Object>`, los endpoints de la v1 no tienen un contrato tipado de la respuesta; al probar la API en Swagger, el "Example Value" de las respuestas aparece vacío (`{}`) (observación del autor), y los códigos de respuesta figuran como *Undocumented* (se ve en `capturas/02-post-201.png` y `capturas/03-error-409.png`). Captura: `docs/evidencias/antes/capturas/06-example-value-vacio.png` (respuesta 200 con media type `*/*` y Example Value `{}`). La documentación de la API depende de ejemplos escritos a mano, y un cliente no puede generar código a partir de ella.
- **Impacto:** llamadas confusas y propensas a errores de orden; sin DTOs no hay validación declarativa ni documentación automática útil.
- **Riesgo de mantenimiento:** Medio. **Prioridad:** Media.
- **Refactoring propuesto:** **R05** (Introduce Parameter Object: `RegistrarSolicitudRequest`, `NuevaSolicitud`, `FiltroSolicitudes`, y DTOs de respuesta).
- **Detectado por PMD:** Sí, `ExcessiveParameterList` en `SC:403` (7 parámetros, mínimo configurado 5). `Utils.validar` (4 parámetros) no se marca porque está por debajo del mínimo, aunque es un data clump igual.

---

### S06 — Feature Envy

- **Evidencia (`SC:389-401`):**
  ```java
  private boolean puedeCambiar(Solicitud s, String nuevo) {
      String actual = s.getEstado();
      if (actual.equals("PENDIENTE")) {
          if (nuevo.equals("EN_ANALISIS") || nuevo.equals("CANCELADA")) {
              return true;
          }
      } else if (actual.equals("EN_ANALISIS")) {
          if (nuevo.equals("RESUELTA") || nuevo.equals("RECHAZADA")) {
              return true;
          }
      }
      return false;
  }
  ```
- **Ubicación:** `SC:389`.
- **Impacto:** el método solo usa datos de `Solicitud` (`s.getEstado()`); la regla de la máquina de estados vive lejos de la entidad a la que pertenece. Quien lea `Solicitud` no encuentra cuándo puede cambiar de estado.
- **Riesgo de mantenimiento:** Medio. **Prioridad:** Alta (esfuerzo bajo).
- **Refactoring propuesto:** **R03** (Move Method → `EstadoSolicitud.puedeTransicionarA`).
- **Detectado por PMD:** No.

---

### S07 — Switch Statements repetidos (viola OCP)

- **Evidencia 1 — plazos (`Utils.java:7-22`):**
  ```java
  public static int calcularDias(String tipo) {
      switch (tipo) {
          case "CERTIFICADO_ALUMNO_REGULAR":
              return 2;
          case "CAMBIO_COMISION":
              return 3;
          case "REVISION_EXAMEN":
              return 5;
          case "OTRO":
              return 7;
          case "EQUIVALENCIA":
              return 15;
          default:
              return 7;
      }
  }
  ```
- **Evidencia 2 — prioridad por defecto, sobre el mismo `tipo` (`SC:61-69`):**
  ```java
  if (prioridad == null) {
      if (tipo.equals("REVISION_EXAMEN")) {
          prioridad = "ALTA";
      } else if (tipo.equals("CAMBIO_COMISION") || tipo.equals("CERTIFICADO_ALUMNO_REGULAR")) {
          prioridad = "MEDIA";
      } else {
          prioridad = "BAJA";
      }
  }
  ```
- **Ubicación:** `Utils.java:7-22` y `SC:61-69`; además la lista de tipos válidos se repite en `Utils.java:32-33`, `SC:57-58`, `SC:172-173` y `SC:362-367` (ver el costo de agregar `BECA` en `docs/tpi/02-calidad-externa-interna.md` §3).
- **Impacto:** dos decisiones paralelas sobre el mismo `String`, en archivos distintos. Agregar un tipo obliga a modificar ambas, y el `default` / `else` final traga el olvido sin error.
- **Riesgo de mantenimiento:** Medio. **Prioridad:** Media.
- **Refactoring propuesto:** **R04** (Replace Conditional with Polymorphism: `TipoSolicitud.plazoDias()` y `prioridadPorDefecto()`).
- **Detectado por PMD:** No (la complejidad de `crear` sí aparece en S02).

---

### S08 — Anemic Domain Model

- **Evidencia (`model/Solicitud.java`, 132 líneas):** tras los campos y anotaciones (líneas 1-50), el resto del archivo son solo getters y setters:
  ```java
  public String getEstado() {
      return estado;
  }

  public void setEstado(String estado) {
      this.estado = estado;
  }
  ```
  Ningún método de `Solicitud`, `Estudiante` ni `Responsable` contiene una regla de negocio. Por ejemplo, la transición de estado se hace desde el controller con `s.setEstado(nuevo)` (`SC:248`) y la asignación con `s.setResponsable(resp)` (`SC:307`).
- **Ubicación:** `Solicitud.java:53-131`, `Estudiante.java`, `Responsable.java`.
- **Impacto:** las reglas (RN-05 a RN-12) están dispersas por el controller; cualquiera puede dejar una `Solicitud` en un estado inconsistente (por ejemplo `RESUELTA` sin responsable) llamando al setter.
- **Riesgo de mantenimiento:** Medio. **Prioridad:** Media.
- **Refactoring propuesto:** **R03** (la entidad gana `cambiarEstado()`, `asignarResponsable()`, `estaCerrada()`, `estaVencida()`).
- **Detectado por PMD:** No.

---

### S09 — Nombres poco expresivos + Magic Numbers

- **Evidencia 1 — nombres:** `s`, `e`, `r`, `m`, `est`, `aux`, `lista2`, `dato`, `res`, y un método llamado `procesar`. Por ejemplo `SC:181` (`List<Solicitud> lista2`), `SC:196` (`aux`), `SC:298` (`Object dato`), `SC:417` (`private ResponseEntity<Object> procesar(Exception e)`).
- **Evidencia 2 — números mágicos:** `SC:54` y `Utils.java:29`:
  ```java
  if (descripcion == null || descripcion.length() < 10 || descripcion.length() > 500) {
  ```
  y los plazos `2, 3, 5, 7, 15` en `Utils.java:9-20`, y el `/ 2` y `< 1` de `SC:86-90`.
- **Relación con PMD — `ShortVariable` (28 violaciones):**

  | Nombre | Cantidad | Dónde |
  |---|---|---|
  | `s` | 6 | `SC:92, 128, 232, 290, 389, 405` |
  | `e` | 6 | `SC:106, 143, 207, 267, 325, 417` |
  | `r` | 5 | `SC:111, 148, 212, 272, 330` |
  | `m` | 2 | `SoC:30, 44` |
  | `id` | 9 | `SC:126, 229, 287` y los campos/parámetros `id` de las tres entidades |
  | **Total** | **28** | |

  Hay que ser precisos: de las 28, **19** son nombres que realmente no dicen qué son (`s`, `e`, `r`, `m`). Los 9 `id` son una convención aceptable (el propio nombre de la clave) y son falsos positivos de la regla; no se cuentan como smell, pero sí cuentan para el total de 56 de PMD. Hay nombres poco claros que PMD no marca, como `est` (`SC:71`), porque la regla solo se queja de nombres de menos de 3 caracteres.
- **Relación con PMD — `AvoidLiteralsInIfCondition` (1):** `SC:88` (`if (dias < 1)`). Los literales de longitud (10 y 500 en `SC:54`) y los de `Utils.java:29` no fueron marcados: es una limitación de esta regla, que no los detectó dentro de condiciones compuestas con `||`.
- **Impacto:** hay que leer el cuerpo completo para entender la intención; el significado de 10, 500 o 7 solo vive en la cabeza de quien escribió el código.
- **Riesgo de mantenimiento:** Bajo. **Prioridad:** Media.
- **Refactoring propuesto:** **R09** (Rename + Replace Magic Number with Symbolic Constant).
- **Detectado por PMD:** Sí: `ShortVariable` (28) y `AvoidLiteralsInIfCondition` (1).

---

### S10 — Manejo de errores deficiente

- **Evidencia 1 — códigos HTTP dentro del texto del mensaje (`SC:55`, `59`, `73`, `234`, `302`, etc.):**
  ```java
  throw new RuntimeException("404|Estudiante no encontrado: " + legajo);
  ```
- **Evidencia 2 — `catch (Exception)` en cada endpoint (`SC:120`, `157`, `223`, `281`, `339`, `384`):**
  ```java
  } catch (Exception e) {
      return procesar(e);
  }
  ```
- **Evidencia 3 — el parseo del prefijo (`SC:417-432`):**
  ```java
  private ResponseEntity<Object> procesar(Exception e) {
      String msg = e.getMessage();
      int codigo = 500;
      if (msg != null && msg.startsWith("404|")) {
          codigo = 404;
          msg = msg.substring(4);
      } else if (msg != null && msg.startsWith("409|")) {
          ...
      return ResponseEntity.status(codigo).contentType(MediaType.valueOf("text/plain;charset=UTF-8"))
              .body("Error: " + msg);
  ```
- **Evidencia 4 — respuesta de error en texto plano:** `capturas/03-error-409.png` muestra el cuerpo `Error: Transición inválida: CANCELADA -> EN_ANALISIS` con `content-type: text/plain;charset=UTF-8`, mientras las respuestas de éxito son JSON (`capturas/02-post-201.png`, `content-type: application/json`). Un cliente de la API recibe dos formatos distintos y el de error no se puede parsear de forma estructurada.
- **Impacto:** el `catch (Exception)` captura también errores de programación (`NullPointerException`, `ClassCastException`) y los devuelve como un 500 con mensaje poco útil (por ejemplo `Error: null`); el código HTTP depende de un prefijo de texto libre; un error de tipeo en `"409|"` cambia el comportamiento sin que nada falle al compilar.
- **Riesgo de mantenimiento:** Alto. **Prioridad:** Alta.
- **Refactoring propuesto:** **R07** (Replace Error Code with Exception + `@RestControllerAdvice`).
- **Detectado por PMD:** Sí, `AvoidCatchingGenericException`: 6 violaciones (`SC:120, 157, 223, 281, 339, 384`).

---

### S11 — Acoplamiento fuerte / violación de DIP

- **Evidencia 1 — `new` de un colaborador concreto (`SC:251-254`):**
  ```java
  if (s.getEstado().equals("RESUELTA") || s.getEstado().equals("RECHAZADA")) {
      new NotificadorConsola().notificar(s.getEstudiante().getEmail(),
              "Su solicitud " + s.getId() + " fue " + s.getEstado());
  ```
- **Evidencia 2 — el reloj como dependencia oculta:** `LocalDateTime.now()` en 9 lugares de `SC` (`75`, `105`, `142`, `206`, `249`, `266`, `310`, `324`, `377`).
- **Evidencia 3 — inyección en campos (`SC:37-44`):** tres `@Autowired private ...Repository`. El propio IDE avisa "Convert @Autowired field into Constructor Parameter".
- **Evidencia 4 — `@Transactional` en el controller (`SC:34`):**
  ```java
  @RestController
  @Transactional
  public class SolicitudController {
  ```
  El controller (capa web) conoce y configura la infraestructura transaccional. Está ahí porque `Solicitud.responsable` es `LAZY` y el proyecto tiene `spring.jpa.open-in-view=false`: sin una transacción abierta, leer `s.getResponsable()` en el controller lanzaría `LazyInitializationException`. Es un acoplamiento a infraestructura y también una señal de que la lógica de negocio está en la capa equivocada: la transacción pertenece a un servicio de aplicación.
- **Evidencia 5 — `Utils` estático (`Utils.java`):** `Utils.calcularDias`, `Utils.validar` y `Utils.calcularVencimiento` se invocan como estáticos (`SC:70`, `85`, `411`), así que no se pueden sustituir ni simular.
- **Impacto:** no se puede probar el cierre de una solicitud sin que escriba en la consola, ni fijar la hora para probar un vencimiento (el test de caracterización CP-15 depende de que los datos semilla estén calculados contra el reloj real); no se puede cambiar el canal de notificación sin modificar el controller.
- **Riesgo de mantenimiento:** Medio. **Prioridad:** Media.
- **Refactoring propuesto:** **R08** (Introduce Interface + DI: `Notificador`, `Clock`, inyección por constructor) y **R06** (la transacción pasa al service).
- **Detectado por PMD:** No con las reglas configuradas (el IDE señala la inyección por campo).

---

### S12 — Dead Code / Comentarios / `System.out`

- **Evidencia 1 — código comentado de una versión vieja del cálculo (`SC:76-84`):**
  ```java
  // int dias = 2;
  // if (tipo.equals("REVISION_EXAMEN")) {
  //     dias = 5;
  // } else if (tipo.equals("CAMBIO_COMISION")) {
  //     dias = 3;
  // }
  // if (prioridad.equals("ALTA")) {
  //     dias = dias - 1;
  // }
  ```
  Además, ese bloque contradice la regla vigente (la prioridad ALTA divide el plazo, no resta un día), lo que genera dudas sobre cuál es la correcta.
- **Evidencia 2 — `System.out.println` de depuración:** `SC:53` (`"DEBUG crear solicitud ..."`) y `SC:413` (`"DEBUG solicitud armada ..."`). A esto se suma `NotificadorConsola.java:6` (la notificación se hace con `System.out.println`).
- **Impacto:** ruido; confusión sobre qué es el código vigente; los logs por consola no tienen nivel ni formato y no se pueden desactivar.
- **Riesgo de mantenimiento:** Bajo. **Prioridad:** Baja.
- **Refactoring propuesto:** **R09** (Remove Dead Code) y uso de SLF4J en R08.
- **Detectado por PMD:** Sí, `SystemPrintln`: 3 violaciones (`SC:53`, `SC:413`, `NotificadorConsola.java:6`). El código comentado no lo detecta.

---

## 3. Matriz de priorización (impacto × esfuerzo)

| | **Esfuerzo bajo** | **Esfuerzo medio** | **Esfuerzo alto** |
|---|---|---|---|
| **Impacto alto** | S03 (R01), S06 (R03) — *ganancias rápidas* | S02 (R01), S04 (R02), S10 (R07) | S01 (R06) |
| **Impacto medio** | S07 (R04), S09 (R09) | S05 (R05), S08 (R03), S11 (R08) | — |
| **Impacto bajo** | S12 (R09) | — | — |

Orden de ataque sugerido (coincide con el orden R01→R09 del plan): primero lo que reduce riesgo con poco esfuerzo (S03, S06, S02), después los tipos y la entrada (S04, S07, S05), después la estructura (S01, S08) y al final errores, dependencias y limpieza (S10, S11, S09, S12).

| Smell | Impacto | Riesgo | Prioridad | Refactoring |
|---|---|---|---|---|
| S01 God Class | Alto | Alto | Alta | R06 |
| S02 Long Method | Alto | Alto | Alta | R01 |
| S03 Duplicated Code | Alto | Alto | Alta | R01 |
| S04 Primitive Obsession | Alto | Alto | Alta | R02 |
| S05 Long Parameter List | Medio | Medio | Media | R05 |
| S06 Feature Envy | Medio | Medio | Alta | R03 |
| S07 Switch repetido | Medio | Medio | Media | R04 |
| S08 Anemic Domain Model | Medio | Medio | Media | R03 |
| S09 Nombres y números mágicos | Bajo | Bajo | Media | R09 |
| S10 Manejo de errores | Alto | Alto | Alta | R07 |
| S11 Acoplamiento / DIP | Medio | Medio | Media | R08 |
| S12 Dead Code / `System.out` | Bajo | Bajo | Baja | R09 |

## 4. Cobertura del diagnóstico por herramienta

| Smell | ¿PMD lo detecta? | Regla / cantidad |
|---|---|---|
| S01 | Parcialmente (síntomas) | `CyclomaticComplexity` (clase), `NcssCount`; `GodClass` = 0 |
| S02 | Sí | `NcssCount` 5, `CyclomaticComplexity` 5, `CognitiveComplexity` 3, `NPathComplexity` 3 |
| S03 | No | — (haría falta CPD) |
| S04 | No | — |
| S05 | Parcialmente | `ExcessiveParameterList` 1 |
| S06 | No | — |
| S07 | No | — |
| S08 | No | — |
| S09 | Sí | `ShortVariable` 28, `AvoidLiteralsInIfCondition` 1 |
| S10 | Sí | `AvoidCatchingGenericException` 6 |
| S11 | No | — |
| S12 | Parcialmente | `SystemPrintln` 3 (el código comentado no) |

Seis de los doce smells (S03, S04, S06, S07, S08, S11) no los ve ninguna de las reglas configuradas, lo que muestra que **las herramientas estáticas complementan, pero no reemplazan, el análisis del diseño**.

## 5. Clasificación de la deuda (cuadrante de Fowler)

El cuadrante de la deuda técnica de Martin Fowler cruza dos ejes: **deliberada / inadvertida** y **imprudente / prudente**.

| | **Imprudente** | **Prudente** |
|---|---|---|
| **Deliberada** | *"No tenemos tiempo para diseñar"* | *"Hay que entregar ya y lidiar con las consecuencias"* |
| **Inadvertida** | *"¿Qué es la capa de servicio?"* | *"Ahora sabemos cómo deberíamos haberlo hecho"* |

**Clasificación principal de la v1: deuda imprudente y deliberada.**

- **Deliberada:** el diseño elegido (un controller que lo hace todo, `Map<String, Object>`, `catch (Exception)` con códigos en el texto) no es un descuido de desconocimiento: fue una decisión de resolver rápido, sin capa de servicio ni dominio. Cada atajo se tomó sabiendo que existe una alternativa más ordenada.
- **Imprudente:** no hubo un plan para pagarla. No se separaron responsabilidades, no se tipificaron estados ni tipos, y el costo (agregar un tipo en 6 lugares, errores silenciosos) se acumula con cada cambio. Es la situación típica de un proyecto "hecho con apuro" sin intención de volver a limpiarlo.

**Aclaración honesta para la defensa:** esta deuda fue construida a propósito con fines didácticos para el trabajo. Dentro de la ficción del proyecto ("un sistema hecho con apuro") encaja en el cuadrante imprudente-deliberado; en términos reales, fue introducida de forma intencional para poder diagnosticarla y refactorizarla. Se declara así en el informe.

**Componentes del cuadrante inadvertido:** algunos smells corresponden a la clase de deuda que aparece por desconocimiento del diseño en capas (S08 modelo anémico, S11 acoplamiento a infraestructura con `@Transactional` en el controller): serían imprudentes-inadvertidos en un equipo que no conoce el patrón de servicio. El camino de pago de toda la deuda es el refactoring seguro de la Fase 4, apoyado en los 15 tests de caracterización (la red de seguridad que se construyó antes de tocar nada).
