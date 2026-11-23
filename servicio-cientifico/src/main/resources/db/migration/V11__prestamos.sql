-- HU-04: préstamo de equipo a un grupo. HU-05: un equipo no puede estar prestado dos veces a la vez.
-- HU-24 (historial de préstamos) lee esta misma tabla.

CREATE TABLE prestamo (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    equipo_id      BIGINT       NOT NULL,
    grupo          VARCHAR(120) NOT NULL,
    observacion    VARCHAR(400) NULL,
    prestado_por   VARCHAR(80)  NOT NULL,                       -- nombre de usuario de quien registra el préstamo
    prestado_en    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    devuelto_en    DATETIME(3)  NULL,                           -- NULL mientras el equipo sigue prestado
    devuelto_por   VARCHAR(80)  NULL,
    -- Solo vale algo mientras el préstamo está abierto. Con el UNIQUE de abajo, la base de datos
    -- impide dos préstamos abiertos del mismo equipo aunque dos peticiones lleguen a la vez (HU-05).
    equipo_abierto BIGINT GENERATED ALWAYS AS (IF(devuelto_en IS NULL, equipo_id, NULL)) VIRTUAL,
    CONSTRAINT fk_prestamo_equipo FOREIGN KEY (equipo_id) REFERENCES equipo (id),
    CONSTRAINT uq_prestamo_abierto UNIQUE (equipo_abierto)
);

CREATE INDEX idx_prestamo_equipo ON prestamo (equipo_id, prestado_en);

GRANT SELECT, INSERT, UPDATE ON cientifica.prestamo TO 'biohub_app'@'%';
