-- HU-03: inventario de equipos de campo (GPS, binoculares, cámaras trampa, etc.).
-- Los números V4..V9 se dejan libres para las migraciones de las demás historias.
-- HU-04/05/06/24 agregan sus propias tablas o columnas en migraciones posteriores (V11, V12, ...).

CREATE TABLE equipo (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo      VARCHAR(40)  NOT NULL UNIQUE,     -- placa o serial del Instituto, en mayúsculas
    nombre      VARCHAR(120) NOT NULL,
    tipo        VARCHAR(60)  NOT NULL,
    descripcion VARCHAR(400) NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

GRANT SELECT, INSERT, UPDATE ON cientifica.equipo TO 'biohub_app'@'%';
