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
- **Commit:** _(se completa al terminar la tanda)_
