# Gestión de Solicitudes Académicas

API REST (Java 21 + Spring Boot 4.1) para registrar y seguir trámites académicos de estudiantes: certificados, revisiones de examen, equivalencias y cambios de comisión. Trabajo Práctico Integrador de Metodología de Sistemas II (UTN – TUP a distancia).

Los datos son sintéticos y la base es H2 en memoria.

## Cómo ejecutarlo

Desde la raíz del proyecto (PowerShell o terminal de VS Code):

```powershell
.\mvnw.cmd spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Consola H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:solicitudes`, usuario `sa`, sin contraseña)

## Tests

```powershell
.\mvnw.cmd test
```

## Documentación

Ver la carpeta [docs/](docs/): PRD, TRD, flujos, diseño, esquema del backend y plan de implementación.
