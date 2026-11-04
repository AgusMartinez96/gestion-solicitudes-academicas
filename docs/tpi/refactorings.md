# Refactorings (v1 → v2)

> Rama: `refactor/solicitudes`. Un refactoring = un commit. Antes de cada commit se ejecuta `.\mvnw.cmd test` y deben dar **16 tests en verde** (15 de caracterización + `contextLoads`). **No se modifica `src/test/java/ar/edu/utn/tup/solicitudes/api/`.**
> Las referencias "Antes" corresponden al tag `estado-inicial`; las "Después" al commit indicado en cada ficha.
> `SC` = `controller/SolicitudController.java`.

---

## R01 — Extract Method

- **Smell(s):** S02 (Long Method), S03 (Duplicated Code) · **Principio:** Clean Code (funciones chicas, un solo nivel de abstracción por función, no repetirse).
- **Motivación:** `crear`, `listar`, `reporte`, `cambiarEstado` y `asignarResponsable` eran métodos de 40 a 77 líneas que mezclaban validación, reglas, persistencia y armado de JSON; el mismo bloque de mapeo estaba copiado 5 veces y la condición "solicitud cerrada" 7 veces.
- **Antes (`estado-inicial`):**
  - `SC:47-123` — `crear` de 77 líneas, con la validación, la prioridad por defecto y el plazo escritos en línea:
    ```java
    if (descripcion == null || descripcion.length() < 10 || descripcion.length() > 500) {
        throw new RuntimeException("400|La descripción debe tener entre 10 y 500 caracteres");
    }
    ...
    if (prioridad == null) {
        if (tipo.equals("REVISION_EXAMEN")) {
            prioridad = "ALTA";
        } else if (tipo.equals("CAMBIO_COMISION") || tipo.equals("CERTIFICADO_ALUMNO_REGULAR")) {
    ```
  - `SC:95-118`, `132-155`, `196-219`, `256-279`, `314-337` — el bloque de mapeo copiado 5 veces:
    ```java
    Map<String, Object> res = new LinkedHashMap<>();
    res.put("id", s.getId());
    ...
    res.put("vencida", !(s.getEstado().equals("RESUELTA") || s.getEstado().equals("RECHAZADA")
            || s.getEstado().equals("CANCELADA")) && s.getFechaVencimiento().isBefore(LocalDateTime.now()));
    ```
  - `SC:294-295`, `375-376` — la condición "cerrada" repetida fuera del mapeo.
  - `SC:128`, `232`, `290` — `findById(...).orElse(null)` + 404 repetido en 3 endpoints.
- **Después:**
  - `SC:47-76` — `crear` de 30 líneas que solo delega:
    ```java
    validarDescripcion(descripcion);
    validarTipo(tipo);
    prioridad = resolverPrioridad(tipo, prioridad);
    Utils.validar(descripcion, tipo, prioridad, legajo);
    Estudiante est = buscarEstudiante(legajo);
    ...
    int dias = calcularPlazoDias(tipo, prioridad);
    Solicitud s = armarSolicitud(legajo, tipo, descripcion, prioridad, ahora, dias, est);
    s = solicitudRepository.save(s);
    return ResponseEntity.status(HttpStatus.CREATED).body(aMap(s));
    ```
  - `SC:323-355` — un único `aMap(Solicitud)` con `aMapEstudiante` y `aMapResponsable`; los 5 endpoints lo usan (`return ResponseEntity.ok(aMap(s))`).
  - `SC:318-321` — un único `estaCerrada(Solicitud)`, usado por `aMap`, `asignarResponsable` y `contarVencidas`.
  - `SC:208-233` — `buscarSolicitud`, `buscarEstudiante` y `buscarResponsableActivo`.
  - `SC:235-296` — `validarDescripcion`, `validarTipo`, `validarEstado`, `validarPrioridad`, `validarFiltros`, `resolverPrioridad` y `calcularPlazoDias`.
  - `SC:172-206` — `contarPorEstado`, `contarPorTipo` y `contarVencidas` (el cuerpo de `reporte` pasó de 43 a 13 líneas); `SC:297-312` — `filtrar` (extraído de `listar`).
  - Resultado: `SolicitudController` pasó de 433 a 401 líneas y el cuerpo de los endpoints quedó en 8 a 30 líneas.
- **Pasos seguros aplicados:**
  1. Extraer primero lo que no depende de nada más (`estaCerrada`, `ahoraTruncado`), reemplazando una ocurrencia a la vez.
  2. Extraer `aMap` copiando el bloque tal cual (mismo orden de claves, mismo `null` en `responsable`) y reemplazar los 5 bloques.
  3. Extraer las validaciones, `resolverPrioridad` y `calcularPlazoDias` conservando el orden descripción → tipo → prioridad → legajo.
  4. Extraer `buscar*`, `contar*` y `filtrar` sin cambiar condiciones ni mensajes.
  5. No se tocó ningún texto de error, código HTTP ni orden de evaluación.
- **Verificación:** `.\mvnw.cmd test` → `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`.
- **Fuera de alcance de R01 (se resuelve en otros refactorings):** los `String` de estado/tipo/prioridad (R02), `puedeCambiar` y las reglas en la entidad (R03), `armarSolicitud` de 7 parámetros (R05), `catch (Exception)` (R07), `Utils`/`NotificadorConsola`/`now()` (R08), `System.out`, nombres cortos y el bloque comentado (R09).
- **Commit:** `9232424`

---

## R02 — Replace Type Code with Enum

- **Smell(s):** S04 (Primitive Obsession / Magic Strings) · **Principio:** Clean Code (tipado fuerte, nombres con significado) y base para OCP (R04).
- **Motivación:** `estado`, `tipo` y `prioridad` eran `String`. Cualquier error de tipeo compilaba y fallaba en ejecución, y la lista de valores válidos estaba copiada en cadenas de `equals`.
- **Antes:**
  - `model/Solicitud.java:22, 28, 31` (`estado-inicial`):
    ```java
    private String tipo;
    private String prioridad;
    private String estado;
    ```
  - Cadenas de `equals` sobre los tres valores, por ejemplo `validarTipo` (`SC:241-246` en el commit de R01):
    ```java
    if (tipo == null || !(tipo.equals("CERTIFICADO_ALUMNO_REGULAR") || tipo.equals("REVISION_EXAMEN")
            || tipo.equals("EQUIVALENCIA") || tipo.equals("CAMBIO_COMISION") || tipo.equals("OTRO"))) {
    ```
  - Literales de estado/tipo/prioridad en `controller/` y `util/` (medición de la Fase 3): **49 / 30 / 11**.
- **Después:**
  - Tres enums nuevos en `model/`: `EstadoSolicitud` (`PENDIENTE, EN_ANALISIS, RESUELTA, RECHAZADA, CANCELADA`), `TipoSolicitud` y `Prioridad`, cada uno con `desde(String)` que devuelve `Optional`. Las constantes están declaradas en el mismo orden que exige el contrato del reporte.
  - `model/Solicitud.java:26-42` — los campos son enums persistidos como texto:
    ```java
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private TipoSolicitud tipo;
    ```
  - `SC:242-255` — `parsearTipo`, `parsearEstado` y `parsearPrioridad` reemplazan a las tres cadenas de `equals`:
    ```java
    private TipoSolicitud parsearTipo(String texto) {
        return TipoSolicitud.desde(texto)
                .orElseThrow(() -> new RuntimeException("400|Tipo de solicitud inválido: " + texto));
    }
    ```
  - `SC:303-306` y `SC:342-354` — `estaCerrada` y `puedeCambiar` comparan con `==` contra constantes del enum.
  - `SC:177-197` — `contarPorEstado` y `contarPorTipo` inicializan los contadores recorriendo `values()`, así que ya no repiten los nombres a mano.
  - `SC:308-322` — `aMap` serializa con `.name()`, por lo que el JSON es idéntico.
  - Literales de estado/tipo/prioridad en `controller/` y `util/`: **0 / 12 / 4**. Los restantes están en `Utils` (`calcularDias`, `validar`) y en el bloque comentado; desaparecen en R04, R08 y R09.
- **Cómo se preserva la base y el JSON:**
  - `@Enumerated(EnumType.STRING)` guarda el nombre de la constante, el mismo texto que antes; `data.sql` no cambió.
  - **Hallazgo:** con Hibernate 7 sobre H2, `@Enumerated(EnumType.STRING)` por sí solo genera columnas `enum (...)` nativas en lugar de `varchar`, lo que cambiaba el esquema del contrato (`docs/05-esquema-backend.md` §1: `VARCHAR`). Se agregó `@JdbcTypeCode(SqlTypes.VARCHAR)` para forzar `varchar(40/10/20)`. Se verificó con `logging.level.org.hibernate.SQL=DEBUG` (`tipo varchar(40) not null`, `prioridad varchar(10) not null`, `estado varchar(20) not null`). Hibernate agrega además un `CHECK (... in (...))` por columna, que antes no existía: es una restricción nueva e inocua, porque los únicos valores posibles son los del enum.
  - El JSON no cambia porque `aMap` sigue publicando `String` (`.name()`), y los mensajes de error usan el texto original recibido.
- **Pasos seguros aplicados:**
  1. Crear los enums sin usarlos (compila igual).
  2. Cambiar los tipos de los campos y de los getters/setters de `Solicitud`; el compilador marcó todos los lugares a adaptar.
  3. Reemplazar cada comparación por `==` contra la constante y cada validación por `parsear*`, conservando el orden de evaluación (descripción → tipo → prioridad → legajo) y los mensajes.
  4. Ejecutar los tests y revisar el DDL generado (paso que detectó el `enum` nativo).
- **Verificación:** `.\mvnw.cmd test` → `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`.
- **Fuera de alcance de R02:** `Utils.calcularDias` y la prioridad por defecto siguen como decisiones sobre el tipo (R04); `Utils.validar` todavía recibe `String` (se elimina en R08); el enum todavía no tiene comportamiento (R03 y R04).
- **Commit:** `fb7220c`

---

## R03 — Move Method

- **Smell(s):** S06 (Feature Envy), S08 (Anemic Domain Model) · **Principio:** cohesión y *Tell, Don't Ask* (la entidad decide sobre sus propios datos); sienta la base de SRP para R06.
- **Motivación:** `puedeCambiar(Solicitud, String)` y `estaCerrada(Solicitud)` solo usaban datos de la solicitud y su estado, pero vivían en el controller. La entidad era un contenedor de getters/setters y el controller le "preguntaba" el estado para decidir por ella.
- **Antes:**
  - `SC:389-401` (`estado-inicial`) — la regla de transiciones, fuera de la entidad y con strings:
    ```java
    private boolean puedeCambiar(Solicitud s, String nuevo) {
        String actual = s.getEstado();
        if (actual.equals("PENDIENTE")) {
            if (nuevo.equals("EN_ANALISIS") || nuevo.equals("CANCELADA")) {
    ```
  - `SC:318-321` (commit de R01) — `estaCerrada(Solicitud)` en el controller, con 3 comparaciones.
  - `SC:248-249` y `SC:307-311` (`estado-inicial`) — el controller modificaba la entidad campo por campo, incluida la regla RN-10:
    ```java
    s.setResponsable(resp);
    if (s.getEstado().equals("PENDIENTE")) {
        s.setEstado("EN_ANALISIS");
        s.setFechaActualizacion(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }
    ```
- **Después:**
  - `model/EstadoSolicitud.java:13-31` — la máquina de estados vive en el enum:
    ```java
    public boolean puedeTransicionarA(EstadoSolicitud destino) {
        return switch (this) {
            case PENDIENTE -> destino == EN_ANALISIS || destino == CANCELADA;
            case EN_ANALISIS -> destino == RESUELTA || destino == RECHAZADA;
            case RESUELTA, RECHAZADA, CANCELADA -> false;
        };
    }
    public boolean esCerrado() { ... }
    public boolean esResolucion() { ... }
    public boolean requiereResponsable() { return esResolucion(); }
    ```
  - `model/Solicitud.java:64-86` — la entidad gana comportamiento: `estaCerrada()`, `estaVencida(LocalDateTime ahora)`, `tieneResponsable()`, `cambiarEstado(EstadoSolicitud, LocalDateTime)` y `asignarResponsable(Responsable, LocalDateTime)` (que encapsula RN-10: si estaba `PENDIENTE` pasa a `EN_ANALISIS`).
  - `SC:118-127` y `SC:142-148` — el controller solo delega:
    ```java
    if (!s.getEstado().puedeTransicionarA(nuevo)) { ... }
    if (nuevo.requiereResponsable() && !s.tieneResponsable()) { ... }
    s.cambiarEstado(nuevo, ahoraTruncado());
    ...
    s.asignarResponsable(resp, ahoraTruncado());
    ```
  - Se eliminaron `puedeCambiar` y `estaCerrada` del controller (`SolicitudController` pasó de 401 a 362 líneas). `aMap` y `contarVencidas` usan `s.estaVencida(...)`.
- **Decisiones de alcance:**
  - Los métodos de dominio **no lanzan excepciones**: los chequeos que producen 409 siguen en el controller. Hacer que la entidad lance errores de negocio exige excepciones propias, y eso es R07; lanzar ahora un `RuntimeException("409|...")` desde el dominio acoplaría la entidad al protocolo HTTP.
  - Se agregó `esResolucion()` (no estaba en el diseño) para distinguir "notificar al estudiante" (RN-12) de "requiere responsable" (RN-08), que hoy coinciden en `RESUELTA`/`RECHAZADA` pero son reglas distintas.
- **Pasos seguros aplicados:**
  1. Agregar los métodos nuevos al enum y a la entidad sin usarlos (compila igual).
  2. Reemplazar un uso a la vez en el controller (transición, requiere responsable, cierre, vencida, asignar).
  3. Borrar `puedeCambiar` y `estaCerrada` del controller cuando ya no tenían llamadas.
  4. Conservar el orden de chequeos y los mensajes de error.
- **Verificación:** `.\mvnw.cmd test` → `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`.
- **Commit:** `43c190e`

---

## R04 — Replace Conditional with Polymorphism (comportamiento dentro de `TipoSolicitud`)

- **Smell(s):** S07 (Switch Statements repetidos) · **Principio:** **OCP** (abierto a extensión, cerrado a modificación).
- **Motivación:** dos decisiones paralelas sobre el mismo tipo —el plazo en `Utils.calcularDias` y la prioridad por defecto en un `if/else` del controller— obligaban a modificar varios archivos para agregar un tipo, y el `default`/`else` final tragaba los olvidos sin error.
- **Antes:**
  - `util/Utils.java:7-22` (`estado-inicial`) — `switch` sobre el texto del tipo:
    ```java
    public static int calcularDias(String tipo) {
        switch (tipo) {
            case "CERTIFICADO_ALUMNO_REGULAR":
                return 2;
            ...
            default:
                return 7;
    ```
  - `SC:252-263` y `SC:265-274` (commit de R03) — prioridad por defecto y ajuste del plazo en el controller:
    ```java
    if (tipo == TipoSolicitud.REVISION_EXAMEN) {
        return Prioridad.ALTA;
    }
    if (tipo == TipoSolicitud.CAMBIO_COMISION || tipo == TipoSolicitud.CERTIFICADO_ALUMNO_REGULAR) {
        return Prioridad.MEDIA;
    }
    return Prioridad.BAJA;
    ```
  - `util/Utils.java:28-43` — `Utils.validar(...)`, que revalidaba descripción, tipo, prioridad y legajo con cadenas de `equals`.
- **Después:**
  - `model/TipoSolicitud.java:7-27` — cada constante declara su plazo y su prioridad por defecto:
    ```java
    CERTIFICADO_ALUMNO_REGULAR(2, Prioridad.MEDIA),
    REVISION_EXAMEN(5, Prioridad.ALTA),
    EQUIVALENCIA(15, Prioridad.BAJA),
    CAMBIO_COMISION(3, Prioridad.MEDIA),
    OTRO(7, Prioridad.BAJA);
    ...
    public int plazoDias() { return plazoDias; }
    public Prioridad prioridadPorDefecto() { return prioridadPorDefecto; }
    ```
  - `model/Prioridad.java:11-16` — la regla "ALTA reduce el plazo a la mitad, mínimo 1 día" vive en la prioridad:
    ```java
    public int ajustarPlazo(int dias) {
        if (this == ALTA) {
            return Math.max(1, dias / 2);
        }
        return dias;
    }
    ```
  - `SC:251-260` — el controller solo delega: `return tipo.prioridadPorDefecto();` y `return prioridad.ajustarPlazo(tipo.plazoDias());`.
  - `Utils.calcularDias` desapareció y `Utils.validar` se eliminó: tras R02, la descripción, el tipo y la prioridad ya se validaban antes de llamarla, y el legajo nulo se resuelve igual en `buscarEstudiante`. `Utils` quedó con un solo método (`calcularVencimiento`) hasta R08.
- **Por qué ahora se cumple OCP — costo de agregar el tipo `BECA`:**

  En la v1 (`docs/tpi/02-calidad-externa-interna.md` §3) agregar `BECA` obligaba a tocar **6 lugares en 2 archivos**, y 2 de ellos fallaban en silencio. Estado actual de cada uno:

  | # | Lugar en la v1 | Qué pasa ahora |
  |---|---|---|
  | 1 | `Utils.java:7-22` — `case "BECA"` (si se olvidaba: plazo 7 por el `default`, **sin error**) | Ya no existe: el plazo es un argumento obligatorio del constructor de la constante. No se puede olvidar: no compila. |
  | 2 | `Utils.java:32-33` — validación en `Utils.validar` | `Utils.validar` se eliminó (R04). |
  | 3 | `SC:57-58` — validación del tipo en `crear` | `parsearTipo` usa `TipoSolicitud.desde`, que recorre `values()` (R02). |
  | 4 | `SC:61-69` — prioridad por defecto (si se olvidaba: `BAJA` por el `else`, **sin error**) | Es el segundo argumento del constructor de la constante. No se puede olvidar. |
  | 5 | `SC:172-173` — validación del filtro `tipo` | Mismo `parsearTipo` (R02). |
  | 6 | `SC:362-367` — `porTipo.put("BECA", 0)` (si se olvidaba: `NullPointerException` → 500) | `contarPorTipo` inicializa los contadores con `values()` (R02). |

  **Ahora basta con agregar una constante en `TipoSolicitud`, en 1 archivo:**
  ```java
  OTRO(7, Prioridad.BAJA),
  BECA(10, Prioridad.MEDIA);
  ```
  **Comprobado con la app levantada** (cambio temporal, descartado con `git checkout`, no incluido en el commit): `POST /api/solicitudes` con `"tipo":"BECA"` y sin prioridad respondió **201** con prioridad `MEDIA` y vencimiento a +10 días; `GET /api/solicitudes?tipo=BECA` devolvió la solicitud; y `GET /api/reportes/resumen` incluyó `"BECA":1` en `porTipo`, sin tocar `SolicitudController`. La columna `tipo` aceptó el valor nuevo porque Hibernate regenera su `CHECK` a partir del enum.

  Eso es OCP: el sistema está **abierto a extensión** (nueva constante) y **cerrado a modificación** (el controller, el reporte, los filtros y la persistencia no se editan). Además cambió el modo de fallo: lo que antes fallaba en silencio ahora no se puede omitir.
- **Pasos seguros aplicados:**
  1. Agregar `plazoDias`, `prioridadPorDefecto` y `ajustarPlazo` con los mismos valores que el `switch` y los `if` (2/3/5/7/15 días; REVISION_EXAMEN→ALTA; CAMBIO_COMISION y CERTIFICADO→MEDIA; resto→BAJA).
  2. Reemplazar las dos decisiones del controller por llamadas al enum.
  3. Borrar `Utils.calcularDias` y `Utils.validar` cuando quedaron sin uso, y comprobar por HTTP que los mensajes de error no cambiaron (`POST` sin legajo → 404 `Estudiante no encontrado: null`; prioridad `URGENTE` → 400 `Prioridad inválida: URGENTE`; prioridad vacía → 400 `Prioridad inválida: `), idénticos a la línea base previa al cambio.
- **Verificación:** `.\mvnw.cmd test` → `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`.
- **Fuera de alcance de R04:** los números `2, 3, 5, 7, 15` y el `/ 2` siguen como literales en el enum (son datos del dominio; el `/ 2` pasará a una constante en R09).
- **Commit:** `bbe0574`

---

## R05 — Introduce Parameter Object

- **Smell(s):** S05 (Long Parameter List + Data Clumps; `Map<String,Object>` como cuerpo) · **Principio:** Clean Code (pocos parámetros, datos que viajan juntos en un solo objeto con nombre).
- **Motivación:** `armarSolicitud` recibía 7 parámetros (4 `String` seguidos), `filtrar` recibía 4 filtros sueltos, `Utils.validar` recibía 4 datos que siempre viajan juntos (ya eliminada en R04) y los tres endpoints de escritura recibían un `Map<String, Object>` casteado a mano.
- **Antes (`bbe0574`, commit de R04):**
  - `SC:50`, `113`, `138` — cuerpos como mapas sin tipo, con casteos:
    ```java
    public ResponseEntity<Object> crear(@RequestBody Map<String, Object> body) {
        String legajo = (String) body.get("legajo");
        ...
    Object dato = body.get("responsableId");
    Long idResp = dato == null ? null : Long.valueOf(dato.toString());
    ```
  - `SC:318-319` — `armarSolicitud(String legajo, TipoSolicitud tipo, String descripcion, Prioridad prioridad, LocalDateTime creacion, int dias, Estudiante est)`.
  - `SC:262` — `filtrar(EstadoSolicitud estado, TipoSolicitud tipo, Prioridad prioridad, String legajo)` con cuatro `if` anidados.
- **Después:**
  - `aplicacion/NuevaSolicitud.java` — record `(legajo, tipo, descripcion, prioridad)` ya validado y tipado; calcula `prioridadEfectiva()` (la informada o la del tipo) y `plazoDias()`.
  - `aplicacion/FiltroSolicitudes.java` — record `(estado, tipo, prioridad, legajo)` con `coincideCon(Solicitud)`; reemplaza los 4 `if` anidados por una sola expresión.
  - `web/dto/RegistrarSolicitudRequest.java`, `CambiarEstadoRequest.java`, `AsignarResponsableRequest.java` — records para el cuerpo de entrada (mismos nombres de campo JSON que antes).
  - `SC:55-73` — `crear` recibe el request, lo valida y arma el objeto:
    ```java
    validarDescripcion(request.descripcion());
    TipoSolicitud tipo = parsearTipo(request.tipo());
    Prioridad prioridad = request.prioridad() == null ? null : parsearPrioridad(request.prioridad());
    NuevaSolicitud nueva = new NuevaSolicitud(request.legajo(), tipo, request.descripcion(), prioridad);
    Estudiante est = buscarEstudiante(nueva.legajo());
    ...
    Solicitud s = armarSolicitud(nueva, est, ahora);
    ```
  - `SC:301` — `armarSolicitud(NuevaSolicitud nueva, Estudiante est, LocalDateTime creacion)`: **3 parámetros** (antes 7). Se eliminaron `resolverPrioridad` y `calcularPlazoDias` del controller.
  - `SC:97-101` y `SC:252-260` — `listar` construye un `FiltroSolicitudes` y `filtrar(FiltroSolicitudes)` usa `filtro.coincideCon(s)`.
  - `SC:116` y `SC:141` — `cambiarEstado` y `asignarResponsable` reciben `CambiarEstadoRequest` y `AsignarResponsableRequest`.
  - `SolicitudController` pasó de 348 a 330 líneas.
- **Cómo se preserva el contrato de entrada:**
  - Los records de request usan `String` para `tipo`, `prioridad` y `estado`, **no los enums**. Si fueran enums, un valor inválido (`"BECA"`) fallaría en la deserialización de Jackson con un error genérico de Spring. Con `String`, el valor llega hasta `parsearTipo`/`parsearPrioridad`/`parsearEstado` y produce el mismo `400` con el mismo mensaje (`Tipo de solicitud inválido: BECA`).
  - El orden de validación se mantiene: descripción → tipo → prioridad → legajo (la prioridad se parsea solo si viene informada; la del tipo por defecto no puede fallar). Para `cambiarEstado` se mantiene: solicitud existe (404) → estado válido (400).
  - Comprobado por HTTP con la app levantada (los 12 casos coinciden con el contrato): descripción corta + tipo inválido → `400 La descripción debe tener entre 10 y 500 caracteres`; tipo inválido + prioridad inválida → `400 Tipo de solicitud inválido: BECA`; prioridad inválida + legajo inexistente → `400 Prioridad inválida: URGENTE`; sin tipo → `400 Tipo de solicitud inválido: null`; sin descripción → `400 La descripción debe tener...`; estado `FOO` sobre solicitud `99` → `404 Solicitud no encontrada: 99`; cuerpo `{}` en estado → `400 Estado inválido: null`; `responsableId` ausente → `404 Responsable no encontrado: null`; `responsableId` como texto `"1"` → 200 (igual que antes); filtros inválidos → `400`; filtro combinado → solo la solicitud 5.
- **Diferencias aceptadas (solo en entradas fuera del contrato):** un campo desconocido en el JSON se ignora (igual que antes); un `tipo` o `descripcion` enviado como número se convierte a texto y produce un `400` de dominio, mientras que antes provocaba un `ClassCastException` y un 500. No hay ninguna entrada válida cuyo resultado cambie.
- **Extensión respecto del pedido:** además de los tres objetos pedidos (`RegistrarSolicitudRequest`, `NuevaSolicitud`, `FiltroSolicitudes`) se agregaron `CambiarEstadoRequest` y `AsignarResponsableRequest`, que `docs/04-diseno.md` §3 ya prevé en `web/dto/`; sin ellos quedarían dos endpoints con `Map<String, Object>` y S05 a medias.
- **Pasos seguros aplicados:**
  1. Crear los records sin usarlos (compila igual), directamente en sus paquetes finales (`aplicacion/`, `web/dto/`).
  2. Cambiar la firma de un método por vez (`armarSolicitud`, `filtrar`, y cada endpoint) y dejar que el compilador marque las llamadas.
  3. Reemplazar los `if` anidados por `coincideCon` manteniendo la misma semántica (`null` = sin filtro).
  4. Verificar por HTTP los casos de error con entradas múltiples.
- **Verificación:** `.\mvnw.cmd test` → `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`.
- **Fuera de alcance de R05:** las respuestas siguen siendo `Map<String, Object>` (DTOs de respuesta tipados, y por ende el "Example Value" de Swagger, quedan para después); el contrato de errores sigue siendo el de la v1 (R07).
- **Commit:** `f4d44c2`

### R05 · Parte B — DTOs de respuesta tipados

- **Smell(s):** S05 (falta de contrato tipado: `Map<String, Object>` como respuesta) · **Principio:** Clean Code (tipos con nombre en lugar de estructuras genéricas); contrato explícito para la documentación de la API.
- **Motivación:** la parte A tipó las entradas, pero las respuestas seguían siendo `Map<String, Object>` armados a mano en el mapper. Springdoc no puede inferir el esquema de un `Map<String, Object>`, y por eso el "Example Value" de Swagger salía `{}` (`docs/evidencias/antes/capturas/06-example-value-vacio.png`).
- **Antes (`3b32287`):** `SolicitudMapper.aMap(...)` devolvía `Map<String, Object>` / `List<Map<String, Object>>` (con `LinkedHashMap` y `put` campo por campo) para solicitudes y reporte, y `SoporteController` armaba sus propios `Map` para estudiantes y responsables.
- **Después:**
  - `web/dto/` — records `SolicitudResponse` (con `EstudianteResumen` y `ResponsableResumen` anidados), `ReporteResumenResponse`, `EstudianteResponse` y `ResponsableResponse`. El orden de los componentes del record es el orden del JSON; `tipo`, `prioridad` y `estado` son enums (se serializan por nombre, igual que antes).
  - `web/SolicitudMapper.java` — `aResponse(Solicitud)`, `aResponse(List<Solicitud>)`, `aResponse(ReporteResumen)`, `aResponse(Estudiante)` y `aResponse(Responsable)` reemplazan a los `aMap`.
  - `ReporteController` devuelve `ReporteResumenResponse`; `SoporteController` devuelve `List<EstudianteResponse>` y `List<ResponsableResponse>` mapeados por `SolicitudMapper` (se aprovechó para pasarlo a inyección por constructor).
- **JSON de salida idéntico (verificado, no supuesto):** antes de tocar el código se capturó una línea base de **25 llamadas HTTP** con la app levantada (éxitos, 400/404/409 y errores de framework) y se repitió después del cambio, normalizando solo los timestamps (relativos al arranque). `diff` entre ambas capturas: **0 diferencias**, lo que cubre nombres, orden de campos, `null` presentes en `fechaActualizacion` y `responsable`, fechas ISO sin decimales, y las claves de `porEstado`/`porTipo` completas y en el orden del contrato.
- **Swagger — resultado parcial, y por qué:** `GET /v3/api-docs` ya muestra esquemas tipados para `reporte-controller` (`ReporteResumenResponse`) y `soporte-controller` (`EstudianteResponse`, `ResponsableResponse`); el "Example Value" de esos endpoints deja de ser `{}`. En cambio, los 5 endpoints de `solicitud-controller` siguen mostrando `object`: sus métodos devuelven `ResponseEntity<Object>` porque el bloque `catch (Exception)` responde con texto plano, y mientras eso exista el tipo de retorno no puede ser `SolicitudResponse`. Se resuelve en R07, cuando el manejo de errores sale del controller; la verificación del "Example Value" de `SolicitudResponse` queda documentada en la ficha de R07.
- **Pasos seguros aplicados:** (1) capturar la línea base HTTP; (2) crear los records sin usarlos; (3) cambiar el mapper y los tres controllers; (4) ejecutar los tests; (5) repetir la captura y comparar con `diff`.
- **Verificación:** `.\mvnw.cmd test` → `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`; diff HTTP de 25 llamadas: sin diferencias.
- **Commit:** _(se completa al terminar la tanda)_

---

## R06 — Extract Class

> R06 se hizo en **dos commits**: la parte A extrae las clases (esta sección) y la parte B reorganiza los paquetes por capas (sección siguiente).

### R06 · Parte A — Extraer `SolicitudService`, `ReporteService`, `SolicitudMapper` y `ReporteController`

- **Smell(s):** S01 (God Class), y de rebote S11 (`@Transactional` en el controller) · **Principio:** **SRP** (una razón de cambio por clase).
- **Motivación:** `SolicitudController` mezclaba siete responsabilidades: recibir HTTP, validar, aplicar reglas de negocio, acceder a datos, armar JSON, notificar y calcular el reporte. Swagger lo mostraba: el reporte aparecía dentro de `solicitud-controller` (`capturas/01-swagger-endpoints.png`).
- **Antes (`f4d44c2`, commit de R05):** `SolicitudController` de 330 líneas (433 en `estado-inicial`), con `@Autowired` en tres repositorios, `@Transactional` en la clase, el reporte (`reporte`, `contarPorEstado`, `contarPorTipo`, `contarVencidas`), el mapeo (`aMap`, `aMapEstudiante`, `aMapResponsable`), las búsquedas (`buscarSolicitud`, `buscarEstudiante`, `buscarResponsableActivo`) y la lógica de cada caso de uso.
- **Después — reparto de responsabilidades:**

  | Clase | Responsabilidad única | Líneas |
  |---|---|---|
  | `controller/SolicitudController` | Recibir HTTP, delegar y traducir el error a texto (sigue hasta R07) | **111** (antes 433) |
  | `aplicacion/SolicitudService` | Casos de uso RF-01 a RF-05 y transacción | 144 |
  | `aplicacion/ReporteService` | Resumen estadístico (RF-06) | 64 |
  | `aplicacion/ReporteResumen` | Resultado tipado del reporte (record) | 10 |
  | `web/SolicitudMapper` | Único lugar que convierte request → dominio y dominio → `Map` JSON | 108 |
  | `web/ReporteController` | Endpoint `GET /api/reportes/resumen` | 25 |

  - `SolicitudController` quedó con endpoints de 3 a 8 líneas:
    ```java
    @PostMapping("/api/solicitudes")
    public ResponseEntity<Object> crear(@RequestBody RegistrarSolicitudRequest request) {
        try {
            System.out.println("DEBUG crear solicitud legajo=" + request.legajo() + " tipo=" + request.tipo());
            NuevaSolicitud nueva = mapper.aNuevaSolicitud(request);
            Solicitud s = solicitudService.registrar(nueva);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aMap(s));
    ```
  - `aplicacion/SolicitudService.java:37-100` — `registrar`, `obtener`, `buscar`, `cambiarEstado` y `asignarResponsable`; las reglas ya estaban en el dominio (R03/R04) y ahora el service solo orquesta.
  - Inyección **por constructor** en las clases nuevas y en el controller adelgazado (`SolicitudService`, `SolicitudMapper`), en lugar de `@Autowired` en campos.
  - `@Transactional` se movió del controller a los services (`@Service @Transactional`): la transacción pertenece a la capa de aplicación.
  - El reporte pasó a `ReporteService.resumen()`, que devuelve un `ReporteResumen` con `EnumMap`; el mapper lo convierte al mismo JSON (`porEstado` y `porTipo` en el orden del contrato).
- **Decisión técnica clave — `@EntityGraph` en `SolicitudRepository`:** al sacar la transacción del controller, el mapper lee `responsable` (que es `LAZY` por contrato) con la transacción ya cerrada (`spring.jpa.open-in-view=false`). Para evitar `LazyInitializationException`, `findById` y `findAll` de `SolicitudRepository` se sobrescriben con `@EntityGraph(attributePaths = { "estudiante", "responsable" })`, de modo que la solicitud llega al mapper con sus asociaciones cargadas. La entidad sigue siendo `LAZY`.
- **Hallazgo durante la verificación:** los 16 tests pasaban, pero **no detectan este problema**, porque cada test corre dentro de una transacción con rollback y la sesión nunca se cierra. Con la app levantada, la primera versión (`attributePaths = "responsable"` solamente) devolvió `500 Could not initialize proxy [Estudiante#2] - no session` en `GET /api/solicitudes/{id}`: un `@EntityGraph` de tipo `FETCH` (el predeterminado) trata como `LAZY` cualquier atributo que no esté en el grafo, incluido `estudiante`, que era `EAGER`. Se corrigió incluyendo `estudiante` y `responsable` en el grafo y se volvió a verificar por HTTP (`GET` de una solicitud con y sin responsable, listados filtrados, `PATCH` de estado y de responsable, `POST` y errores 404/409): todas las respuestas 200/201/404/409 correctas y 0 errores de sesión en el log.
- **Comprobación en Swagger:** `GET /v3/api-docs` agrupa ahora los endpoints así: `solicitud-controller` (los 5 de `/api/solicitudes`), `reporte-controller` (`/api/reportes/resumen`) y `soporte-controller` (`/api/estudiantes`, `/api/responsables`). En `01-swagger-endpoints.png` (v1) el reporte figuraba dentro de `solicitud-controller`.
- **Pasos seguros aplicados:**
  1. Crear `ReporteService`/`ReporteController` y mover primero el reporte (lo más independiente).
  2. Crear `SolicitudMapper` y mover el mapeo y las conversiones de entrada (`aNuevaSolicitud`, `aTipo`, `aEstado`, `aPrioridad`).
  3. Crear `SolicitudService` y mover un caso de uso por vez, conservando el orden de chequeos (para `cambiarEstado`, el service recibe el estado como texto porque el orden `404 → 400` forma parte del contrato).
  4. Reducir el controller a delegación y mover `@Transactional` a los services.
  5. Probar por HTTP además de `mvnw test`.
- **Cambios de comportamiento aceptados:** el endpoint de reporte ya no captura `Exception` (no tiene errores de negocio posibles); una falla inesperada devolvería el 500 genérico de Spring en lugar de texto plano. No hay entrada válida que cambie de resultado.
- **Verificación:** `.\mvnw.cmd test` → `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0` + verificación por HTTP descripta arriba.
- **Fuera de alcance de la parte A:** `catch (Exception)` y el parseo de `"404|..."` (R07); `new NotificadorConsola()`, `LocalDateTime.now()`, `Utils` (R08); `System.out`, nombres cortos y el bloque comentado (R09); `SoporteController` sigue con `@Autowired` y mapeo propio.
- **Commit:** `1c8d5af`

### R06 · Parte B — Reorganizar los paquetes por capas

- **Smell(s):** S01 (responsabilidades mezcladas a nivel de módulo) · **Principio:** SRP / modularidad, separación en capas con dependencias hacia el dominio.
- **Motivación:** después de la parte A las clases estaban repartidas entre los paquetes de la v1 (`model`, `repository`, `controller`, `util`) y los nuevos (`aplicacion`, `web`, `web/dto`). La estructura debía coincidir con `docs/04-diseno.md` §3.
- **Antes (`1c8d5af`):**
  ```
  solicitudes/
  ├── model/        Solicitud, Estudiante, Responsable, EstadoSolicitud, TipoSolicitud, Prioridad
  ├── repository/   SolicitudRepository, EstudianteRepository, ResponsableRepository
  ├── controller/   SolicitudController, SoporteController
  ├── util/         Utils, NotificadorConsola
  ├── aplicacion/   (nuevos) SolicitudService, ReporteService, ReporteResumen, NuevaSolicitud, FiltroSolicitudes
  └── web/          (nuevos) ReporteController, SolicitudMapper, dto/
  ```
- **Después:**
  ```
  solicitudes/
  ├── SolicitudesApplication
  ├── dominio/       Solicitud, Estudiante, Responsable, EstadoSolicitud, TipoSolicitud, Prioridad
  ├── aplicacion/    SolicitudService, ReporteService, ReporteResumen, NuevaSolicitud, FiltroSolicitudes
  ├── persistencia/  SolicitudRepository, EstudianteRepository, ResponsableRepository
  ├── web/           SolicitudController, ReporteController, SoporteController, SolicitudMapper
  │   └── dto/       RegistrarSolicitudRequest, CambiarEstadoRequest, AsignarResponsableRequest
  └── util/          Utils, NotificadorConsola   (transitorio)
  ```
  - `model/` → `dominio/`, `repository/` → `persistencia/` y `controller/` → `web/` con `git mv`, de modo que Git registra renombres y conserva el historial de cada archivo; luego se actualizaron los `package` e `import`.
  - Se eliminó el import redundante de `SolicitudMapper` en `SolicitudController`, que ahora comparte paquete con él.
  - Las clases nuevas de la parte A y de R05 ya se habían creado en su paquete definitivo, así que este commit solo mueve clases existentes.
- **Lo que todavía no coincide con §3, y por qué:**
  - `util/` (`Utils`, `NotificadorConsola`) no existe en el diseño final: `Utils` y `NotificadorConsola` desaparecen en R08 (reemplazados por `Clock` y `Notificador`/`NotificadorLog`).
  - `config/` (`ClockConfig`) nace en R08, que introduce el bean `Clock`. `dominio/excepciones/` y `ManejadorGlobalErrores` nacen en R07.
  - Las respuestas siguen siendo `Map<String, Object>` generados por `SolicitudMapper`; los DTOs de respuesta de `web/dto/` (`SolicitudResponse`, etc.) no se crearon todavía.
- **Verificación en Swagger** (contraste con `docs/evidencias/antes/capturas/01-swagger-endpoints.png`, donde `GET /api/reportes/resumen` estaba dentro de `solicitud-controller`). `GET /v3/api-docs` agrupa ahora:

  | Grupo (tag) | Endpoints |
  |---|---|
  | `solicitud-controller` | `GET` y `POST /api/solicitudes`, `GET /api/solicitudes/{id}`, `PATCH /api/solicitudes/{id}/estado`, `PATCH /api/solicitudes/{id}/responsable` |
  | `reporte-controller` | `GET /api/reportes/resumen` |
  | `soporte-controller` | `GET /api/estudiantes`, `GET /api/responsables` |

  📸 [CAPTURA PENDIENTE: Swagger UI con `reporte-controller` separado, para guardar como `docs/evidencias/despues/capturas/01-swagger-endpoints.png`]
- **Pasos seguros aplicados:** (1) mover los archivos con `git mv`; (2) reemplazar los nombres de paquete con una sola sustitución sobre `src/main`; (3) borrar el import redundante; (4) compilar y ejecutar los tests; (5) levantar la app y repetir las pruebas HTTP (GET con y sin responsable, `PATCH` de estado y responsable, `POST`, reporte, soporte), sin errores de sesión en el log.
- **Verificación:** `.\mvnw.cmd test` → `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`. `src/test/` no se modificó.
- **Commit:** `3b32287`

---

## R07 — Replace Error Code with Exception + manejo centralizado

- **Smell(s):** S10 (manejo de errores deficiente) · **Principio:** Clean Code (excepciones en lugar de códigos de retorno; sin `catch` genérico), SRP (la traducción a HTTP vive en un único lugar).
- **Motivación:** cada endpoint envolvía todo en `try { ... } catch (Exception e)`; los errores de negocio eran `new RuntimeException("404|Estudiante no encontrado: ...")` y `procesar(Exception)` parseaba el prefijo para elegir el código HTTP. Un `NullPointerException` real se confundía con un error de negocio y salía como un 500 con `Error: null`.
- **Antes:**
  - `SC:55`, `59`, `73`, `234`, `302` (`estado-inicial`) — el código HTTP dentro del texto:
    ```java
    throw new RuntimeException("404|Estudiante no encontrado: " + legajo);
    ```
  - `SC:120`, `157`, `223`, `281`, `339`, `384` — `catch (Exception e) { return procesar(e); }` en cada endpoint (6 violaciones PMD `AvoidCatchingGenericException`).
  - `SC:417-432` — `procesar` parseando `"404|"`, `"409|"`, `"400|"` y devolviendo `text/plain`.
- **Después:**
  - `dominio/excepciones/` — `DatoInvalidoException` (→ 400), `RecursoNoEncontradoException` (→ 404) y `ReglaDeNegocioException` (→ 409), todas con el mismo texto de mensaje que antes (sin prefijo).
  - `web/dto/ErrorResponse.java` — `record ErrorResponse(int status, String error, String mensaje, LocalDateTime timestamp)`.
  - `web/ManejadorGlobalErrores.java` — único `@RestControllerAdvice`: un `@ExceptionHandler` por excepción propia y uno para `Exception` (error inesperado).
  - `SolicitudService`, `SolicitudMapper` y `Solicitud` lanzan las excepciones propias. Además, **la regla de transición y la de "requiere responsable" pasaron a `Solicitud.cambiarEstado(...)`**, que ahora protege su propia invariante (en R03 solo había mutadores porque todavía no existían las excepciones de dominio).
  - `web/SolicitudController` quedó sin `try/catch` ni `procesar`, y con tipos de retorno reales (`SolicitudResponse`, `List<SolicitudResponse>`, `201` con `@ResponseStatus`). El controller pasó de 111 a 71 líneas.
  - Resultado: `grep` en `src/main` de `catch (Exception`, `new RuntimeException(` y de los prefijos `"404|"`, `"409|"`, `"400|"` → **0 coincidencias**.
- **Cambio externo intencional (el único permitido por `CLAUDE.md` §3.2): el cuerpo de error pasa de texto plano a JSON.** Código HTTP y mensaje son idénticos.

  | | Antes (v1, `docs/evidencias/antes/capturas/03-error-409.png`) | Después (v2) |
  |---|---|---|
  | Código | `409` | `409` |
  | `Content-Type` | `text/plain;charset=UTF-8` | `application/json;charset=UTF-8` |
  | Cuerpo | `Error: Transición inválida: CANCELADA -> EN_ANALISIS` | `{"status":409,"error":"Conflict","mensaje":"Transición inválida: CANCELADA -> EN_ANALISIS","timestamp":"2026-11-03T22:14:07"}` |

  La captura de la v1 muestra el cuerpo en texto plano y `content-type: text/plain;charset=UTF-8`; los clientes de la v2 reciben el mismo tipo de contenido (JSON) para éxitos y errores. Los tests de caracterización verifican el código HTTP y que el cuerpo **contenga** el mensaje, por eso pasan sin modificarse. Para que esa verificación funcione con tildes, el `Content-Type` de error declara `charset=UTF-8` explícitamente.
- **Errores de framework (JSON malformado, id no numérico, ruta inexistente):** `ManejadorGlobalErrores` extiende `ResponseEntityExceptionHandler`, de modo que los errores que Spring ya resolvía con 400/404 **conservan su código** y ahora usan el mismo formato `ErrorResponse`. Un `@ExceptionHandler(Exception.class)` a secas habría convertido esos casos en 500. El mensaje se toma del `ProblemDetail` de Spring (por ejemplo `Failed to read request`) y nunca de la excepción interna.
- **Errores inesperados (500):** se registran con `LOG.error` (con la traza) y se responden con `{"status":500,"error":"Internal Server Error","mensaje":"Error interno del servidor",...}`, **sin exponer detalles internos**. Comprobado forzando temporalmente una `IllegalStateException("detalle-interno-secreto jdbc:h2:mem")` en un endpoint (el cambio se revirtió y no se incluyó en el commit): el cliente recibió solo el mensaje genérico y el texto secreto apareció únicamente en el log del servidor.
- **Verificación por HTTP (25 llamadas, comparadas con la línea base):** las 10 llamadas de éxito son idénticas byte a byte; las 15 llamadas de error (5 de tipo 400, 3 de tipo 404, 4 de tipo 409 y 3 errores de framework) mantienen su código HTTP y solo cambian el `Content-Type` y la forma del cuerpo.
- **Swagger:** con el `try/catch` fuera de los controllers, los métodos devuelven tipos reales. `GET /v3/api-docs` ahora expone para los 8 endpoints un esquema tipado (`SolicitudResponse` en los 5 de solicitudes con sus 11 campos en el orden del contrato, `ReporteResumenResponse`, `EstudianteResponse`, `ResponsableResponse`), y `POST /api/solicitudes` documenta el `201`. Esto cierra la verificación del "Example Value" que quedó pendiente en R05 parte B: ya no es `{}`.
- **Pasos seguros aplicados:** (1) crear las excepciones y el handler sin usarlos; (2) reemplazar `RuntimeException("4xx|...")` por la excepción correspondiente, una capa por vez (mapper, servicio, entidad); (3) recién entonces quitar los `try/catch` y `procesar` del controller; (4) correr tests y repetir la batería HTTP.
- **Verificación:** `.\mvnw.cmd test` → `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`.
- **Fuera de alcance de R07:** `timestamp` usa `LocalDateTime.now()` hasta R08 (Clock); `Solicitud.asignarResponsable` no valida cerrada/inactivo: esas reglas siguen en el servicio porque el contrato exige el orden 409 (cerrada) → 404 (responsable) → 409 (inactivo), que intercala una búsqueda entre dos reglas.
- **Commit:** _(se completa al terminar la tanda)_
