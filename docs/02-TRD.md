# 02 — TRD: Requisitos técnicos

## 1. Stack

| Componente | Elección | Motivo |
|---|---|---|
| Lenguaje | **Java 21** (si el autor tiene 17 instalado, usar 17: no cambia nada del diseño) | Tipado fuerte, SOLID se ve claro. |
| Framework | **Spring Boot 4.1.x** (el proyecto se generó con 4.1.2 en start.spring.io) | Estándar del mercado y del CV del autor. |
| Build | **Maven con wrapper** (`mvnw.cmd`) | No requiere instalar Maven. |
| Persistencia | **Spring Data JPA + H2 en memoria** | Cero instalación; la consigna admite "base de datos o memoria estructurada". |
| Validación | Validación **manual** en v1 y en v2, centralizada en `NuevaSolicitud` y en los métodos `parsear` de los enums (sin `spring-boot-starter-validation`) | Bean Validation acumula errores y no garantiza el orden ni los mensajes exactos del contrato HTTP, que forman parte de los tests de caracterización. Ver [ADR-005](tpi/decisiones/ADR-005-validacion-manual.md). |
| Documentación API | **springdoc-openapi-starter-webmvc-ui 3.2.0** (la línea 3.x es la compatible con Spring Boot 4; la 2.x es para Boot 3 y NO funciona; la 3.2.0 está construida sobre Boot 4.1.2) | Swagger UI para la demo y el video. |
| Tests | **JUnit 5 + MockMvc + AssertJ** (incluidos en `spring-boot-starter-test`) | Tests de caracterización a nivel HTTP + unitarios de dominio. |
| Cobertura | **JaCoCo** (`jacoco-maven-plugin` 0.8.16) | Evidencia de cobertura antes/después. |
| Análisis estático | **PMD** (`maven-pmd-plugin` 3.28.0, que usa PMD 7.17.0) con ruleset `quickstart` + reglas de diseño | Evidencia cuantitativa de smells antes/después ("informe de herramientas"). |
| Lombok | **NO usar** | Se quiere ver explícitamente constructores, getters y responsabilidades. |

## 1.1 Atención: Spring Boot 4 (no 3)

Spring Boot 4 tiene cambios que rompen código escrito "de memoria" para Boot 3. Reglas:
- **No asumir paquetes ni starters de Boot 3.** Antes de usar una anotación o clase de test/autoconfiguración, verificar que exista en las dependencias reales (compilar con `.\mvnw.cmd test-compile` y corregir).
- Boot 4 está **modularizado**: los starters se llaman por tecnología (por ejemplo `spring-boot-starter-webmvc`) y los tests usan starters específicos (por ejemplo `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`). Respetar lo que generó start.spring.io en el `pom.xml` y agregar solo lo que falte.
- `@AutoConfigureMockMvc` cambió de paquete en Boot 4 (ahora está en el módulo de test de webmvc). Usar el import que corresponda a la versión del pom.
- La **consola H2** en Boot 4 es un módulo aparte (`spring-boot-h2console`); si `spring.h2.console.enabled=true` no la levanta, agregar esa dependencia. Es opcional: si da problemas, se puede omitir.
- **Jackson 3** (paquetes `tools.jackson.*` en lugar de `com.fasterxml.jackson.databind.*`; las anotaciones `com.fasterxml.jackson.annotation.*` se mantienen). Las fechas `LocalDateTime` se serializan en ISO-8601 por defecto.
- Jakarta EE 11 / Hibernate 7: los imports siguen siendo `jakarta.persistence.*` (la v2 no usa `jakarta.validation.*`, ver ADR-005).

## 2. Coordenadas del proyecto

- `groupId`: `ar.edu.utn.tup`
- `artifactId`: `gestion-solicitudes-academicas`
- Paquete base: `ar.edu.utn.tup.solicitudes`
- Puerto: `8080`

## 3. Configuración (`src/main/resources/application.properties`)

```properties
spring.application.name=gestion-solicitudes-academicas
spring.datasource.url=jdbc:h2:mem:solicitudes;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always
spring.h2.console.enabled=true
spring.jpa.open-in-view=false
springdoc.swagger-ui.path=/swagger-ui.html
```

Datos sintéticos en `src/main/resources/data.sql` (ver `05-esquema-backend.md` §4).

## 4. Configuración de PMD

Archivo `config/pmd/ruleset.xml`, referenciado desde el `pom.xml`. Debe incluir como mínimo:
- `category/java/design.xml/GodClass`
- `category/java/design.xml/CyclomaticComplexity`
- `category/java/design.xml/NPathComplexity`
- `category/java/design.xml/ExcessiveParameterList` (minimum = 5)
- `category/java/design.xml/CognitiveComplexity`
- `category/java/design.xml/NcssCount` (methodReportLevel = 30)
- `category/java/bestpractices.xml/AvoidReassigningParameters`
- `category/java/errorprone.xml/AvoidCatchingGenericException` (o su equivalente en la versión de PMD)
- `category/java/codestyle.xml/ShortVariable`
- `category/java/errorprone.xml/AvoidLiteralsInIfCondition`
- `category/java/bestpractices.xml/SystemPrintln`

Configurar el plugin con `failOnViolation=false` (es para medir, no para cortar el build) y `printFailingErrors=true`.

## 5. Convenciones de código (aplican a la v2)

- Nombres en español para el dominio (`Solicitud`, `Estudiante`, `Responsable`), en inglés para lo técnico (`Repository`, `Controller`, `Service`, `Mapper`).
- Métodos de 20 líneas como máximo; idealmente menos de 10.
- Hasta 3 parámetros por método; si hay más, usar Parameter Object.
- Sin `System.out.println`: usar SLF4J (`LoggerFactory.getLogger`).
- Excepciones de negocio propias (`SolicitudNoEncontradaException`, `TransicionInvalidaException`, etc.) y un solo `@RestControllerAdvice`.
- DTOs como `record` de Java.
- Inyección por constructor (sin `@Autowired` en campos).
- Tiempo inyectado mediante `java.time.Clock` (bean), nunca `LocalDateTime.now()` suelto.

## 6. Estructura de carpetas del repositorio

```
gestion-solicitudes-academicas/
├── CLAUDE.md
├── README.md
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/
├── config/pmd/ruleset.xml
├── src/main/java/ar/edu/utn/tup/solicitudes/...
├── src/main/resources/{application.properties, data.sql}
├── src/test/java/ar/edu/utn/tup/solicitudes/
│   ├── api/          ← tests de caracterización (NO se tocan en el refactoring)
│   └── dominio/      ← tests unitarios (se agregan en v2)
└── docs/
    ├── 01-PRD.md … 06-plan-implementacion.md   ← documentación para Claude Code
    ├── tpi/                                    ← entregables del TPI
    │   ├── 01-sistema-base.md
    │   ├── 02-calidad-externa-interna.md
    │   ├── 03-deuda-tecnica-code-smells.md
    │   ├── 04-solid-clean-code.md
    │   ├── refactorings.md
    │   ├── 06-pruebas-verificacion.md
    │   ├── 07-code-review-checklist.md
    │   ├── 08-conclusiones.md
    │   └── decisiones/ADR-001..005
    ├── pruebas/casos-de-prueba.md
    ├── evidencias/
    │   ├── antes/   (reportes surefire/jacoco/pmd y capturas de v1)
    │   └── despues/ (lo mismo para v2)
    └── ia/registro-prompts.md
```

## 7. Entorno del autor
- Windows 10, VS Code con la extensión de Claude Code, terminal PowerShell.
- Git y cuenta de GitHub. JDK 21 (o 17) instalado.
- Verificar el JDK con `java -version` en la terminal de VS Code.
