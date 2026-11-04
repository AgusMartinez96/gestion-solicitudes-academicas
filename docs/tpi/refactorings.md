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
