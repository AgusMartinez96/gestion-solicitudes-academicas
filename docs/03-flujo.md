# 03 — Flujos

## 1. Máquina de estados de una solicitud

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE : registrar (RF-01)
    PENDIENTE --> EN_ANALISIS : cambiar estado / asignar responsable (RN-10)
    PENDIENTE --> CANCELADA : cambiar estado
    EN_ANALISIS --> RESUELTA : cambiar estado [tiene responsable] (RN-08)
    EN_ANALISIS --> RECHAZADA : cambiar estado [tiene responsable] (RN-08)
    RESUELTA --> [*]
    RECHAZADA --> [*]
    CANCELADA --> [*]
```

Estados cerrados: `RESUELTA`, `RECHAZADA`, `CANCELADA`.

## 2. Flujo: registrar solicitud (RF-01)

```mermaid
flowchart TD
    A[POST /api/solicitudes] --> B{¿descripción válida? 10-500}
    B -- no --> E400[400 Bad Request]
    B -- sí --> C{¿tipo válido?}
    C -- no --> E400
    C -- sí --> D{¿prioridad informada?}
    D -- sí --> D2{¿prioridad válida?}
    D2 -- no --> E400
    D2 -- sí --> F
    D -- no --> D3[prioridad por defecto según tipo RN-04] --> F
    F{¿existe estudiante con ese legajo?} -- no --> E404[404 Not Found]
    F -- sí --> G[estado = PENDIENTE, fechaCreacion = ahora]
    G --> H[fechaVencimiento = creación + plazo RN-06]
    H --> I[guardar] --> J[201 Created + JSON solicitud]
```

> **Orden de validación (importante, forma parte del contrato):** descripción → tipo → prioridad → legajo. v1 y v2 deben validar en el mismo orden para devolver el mismo error ante una entrada con varios problemas.

## 3. Flujo: cambiar estado (RF-04)

```mermaid
flowchart TD
    A[PATCH /api/solicitudes/id/estado] --> B{¿existe solicitud?}
    B -- no --> E404[404]
    B -- sí --> C{¿estado destino es un valor válido?}
    C -- no --> E400[400]
    C -- sí --> D{¿transición permitida? RN-07}
    D -- no --> E409[409 Conflict]
    D -- sí --> F{¿destino RESUELTA/RECHAZADA sin responsable? RN-08}
    F -- sí --> E409
    F -- no --> G[actualizar estado y fechaActualizacion]
    G --> H{¿destino RESUELTA/RECHAZADA?}
    H -- sí --> N[notificar estudiante - log] --> OK[200 + JSON]
    H -- no --> OK
```

## 4. Flujo: asignar responsable (RF-05)

```mermaid
flowchart TD
    A[PATCH /api/solicitudes/id/responsable] --> B{¿existe solicitud?}
    B -- no --> E404[404]
    B -- sí --> C{¿solicitud cerrada?}
    C -- sí --> E409[409]
    C -- no --> D{¿existe responsable?}
    D -- no --> E404
    D -- sí --> E{¿responsable activo?}
    E -- no --> E409
    E -- sí --> F[asignar responsable]
    F --> G{¿estado PENDIENTE?}
    G -- sí --> H[estado = EN_ANALISIS RN-10] --> OK[200 + JSON]
    G -- no --> OK
```

## 5. Flujo: reporte resumen (RF-06)

1. Obtener todas las solicitudes.
2. Contar por estado: incluir **todos** los estados, con 0 si no hay solicitudes.
3. Contar por tipo: incluir **todos** los tipos, con 0 si no hay solicitudes.
4. Contar vencidas (RN-11).
5. Devolver `200` con `{ total, porEstado, porTipo, vencidas }`.

## 6. Flujo de trabajo del TPI (proceso, no sistema)

```mermaid
flowchart LR
    V1[v1 con deuda] -->|tag estado-inicial| T[tests de caracterización<br/>verdes en v1]
    T --> DX[diagnóstico: 8+ smells<br/>+ PMD antes]
    DX -->|tag etapa-diagnostico| RF[refactorings R01..R08<br/>1 commit c/u, tests verdes]
    RF --> PR[Pull Request + code review<br/>con checklist]
    PR -->|merge, tag etapa-refactoring| PT[tests unitarios nuevos<br/>+ JaCoCo/PMD después]
    PT -->|tag etapa-pruebas| DOC[README + docs TPI]
    DOC -->|tag etapa-docs| FIN[tag estado-refactorizado]
```
