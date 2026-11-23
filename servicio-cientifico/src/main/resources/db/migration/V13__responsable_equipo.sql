-- HU-06 y HU-07: investigador responsable de un equipo, con historial (se cierra, nunca se borra).
-- Mientras "hasta" sea NULL la asignacion esta vigente. La columna generada equipo_vigente deja a la base
-- de datos como ultima barrera: un equipo no puede tener dos responsables vigentes al mismo tiempo.

CREATE TABLE asignacion_responsable (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    equipo_id      BIGINT       NOT NULL,
    usuario_id     BIGINT       NOT NULL,
    asignado_por   VARCHAR(80)  NOT NULL,
    asignado_en    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    hasta          DATETIME(3)  NULL,
    equipo_vigente BIGINT GENERATED ALWAYS AS (IF(hasta IS NULL, equipo_id, NULL)) VIRTUAL,
    CONSTRAINT fk_asignacion_equipo  FOREIGN KEY (equipo_id)  REFERENCES equipo (id),
    CONSTRAINT fk_asignacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT uq_asignacion_vigente UNIQUE (equipo_vigente)
);

GRANT SELECT, INSERT, UPDATE ON cientifica.asignacion_responsable TO 'biohub_app'@'%';
