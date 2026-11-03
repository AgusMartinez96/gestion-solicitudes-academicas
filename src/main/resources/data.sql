INSERT INTO estudiante (legajo, nombre, email) VALUES ('L-1001', 'Lucía Fernández', 'lucia.fernandez@alumnos.example');
INSERT INTO estudiante (legajo, nombre, email) VALUES ('L-1002', 'Tomás Rivero', 'tomas.rivero@alumnos.example');
INSERT INTO estudiante (legajo, nombre, email) VALUES ('L-1003', 'Martina Sosa', 'martina.sosa@alumnos.example');

INSERT INTO responsable (nombre, area, activo) VALUES ('Carla Gómez', 'Bedelía', TRUE);
INSERT INTO responsable (nombre, area, activo) VALUES ('Diego Paz', 'Departamento Alumnos', TRUE);
INSERT INTO responsable (nombre, area, activo) VALUES ('Hernán Ruiz', 'Secretaría Académica', FALSE);

INSERT INTO solicitud (tipo, descripcion, prioridad, estado, fecha_creacion, fecha_vencimiento, estudiante_id, responsable_id)
VALUES ('CERTIFICADO_ALUMNO_REGULAR', 'Necesito el certificado para presentar en el trabajo', 'MEDIA', 'PENDIENTE',
        DATEADD('DAY', -10, CURRENT_TIMESTAMP(0)), DATEADD('DAY', -8, CURRENT_TIMESTAMP(0)), 1, NULL);
INSERT INTO solicitud (tipo, descripcion, prioridad, estado, fecha_creacion, fecha_vencimiento, estudiante_id, responsable_id)
VALUES ('REVISION_EXAMEN', 'Solicito revisión del parcial de Programación III', 'ALTA', 'EN_ANALISIS',
        DATEADD('DAY', -1, CURRENT_TIMESTAMP(0)), DATEADD('DAY', 1, CURRENT_TIMESTAMP(0)), 2, 1);
INSERT INTO solicitud (tipo, descripcion, prioridad, estado, fecha_creacion, fecha_vencimiento, estudiante_id, responsable_id)
VALUES ('EQUIVALENCIA', 'Pido equivalencia de Matemática I cursada en otra facultad', 'BAJA', 'RESUELTA',
        DATEADD('DAY', -20, CURRENT_TIMESTAMP(0)), DATEADD('DAY', -5, CURRENT_TIMESTAMP(0)), 1, 2);
INSERT INTO solicitud (tipo, descripcion, prioridad, estado, fecha_creacion, fecha_vencimiento, estudiante_id, responsable_id)
VALUES ('CAMBIO_COMISION', 'Quiero pasar a la comisión del turno noche por trabajo', 'MEDIA', 'CANCELADA',
        DATEADD('DAY', -10, CURRENT_TIMESTAMP(0)), DATEADD('DAY', -7, CURRENT_TIMESTAMP(0)), 3, NULL);
INSERT INTO solicitud (tipo, descripcion, prioridad, estado, fecha_creacion, fecha_vencimiento, estudiante_id, responsable_id)
VALUES ('OTRO', 'Consulta sobre el calendario académico del próximo cuatrimestre', 'BAJA', 'PENDIENTE',
        DATEADD('DAY', -1, CURRENT_TIMESTAMP(0)), DATEADD('DAY', 6, CURRENT_TIMESTAMP(0)), 3, NULL);
