-- HU-26: alertas que genera el monitor de seguridad (ASR-4). Solo de agregar, como el registro de accesos.
CREATE TABLE alerta_seguridad (
    id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT      NOT NULL,
    tipo            ENUM('CONSULTAS_EXCESIVAS','RECHAZOS_REPETIDOS') NOT NULL,
    cantidad        INT         NOT NULL,
    ventana_minutos INT         NOT NULL,
    generada_en     DATETIME(3) NOT NULL,
    enviada         BOOLEAN     NOT NULL,   -- si el correo salió; si falla, la alerta queda guardada igual
    INDEX idx_alerta_usuario_tipo (usuario_id, tipo, generada_en),
    CONSTRAINT fk_alerta_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TRIGGER trg_alerta_no_update BEFORE UPDATE ON alerta_seguridad
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alerta_seguridad es de solo agregar: UPDATE no permitido';
CREATE TRIGGER trg_alerta_no_delete BEFORE DELETE ON alerta_seguridad
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alerta_seguridad es de solo agregar: DELETE no permitido';

GRANT SELECT, INSERT ON cientifica.alerta_seguridad TO 'biohub_app'@'%';
