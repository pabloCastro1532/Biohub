-- HU-27: acciones del responsable de seguridad (revocar y consultar exposición). Solo se agrega.
CREATE TABLE accion_seguridad (
    id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tipo            ENUM('REVOCACION','CONSULTA_EXPOSICION') NOT NULL,
    responsable_id  BIGINT      NOT NULL,
    usuario_id      BIGINT      NOT NULL,   -- el usuario revocado o consultado
    ocurrida_en     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_accion_usuario (usuario_id, tipo, ocurrida_en),
    CONSTRAINT fk_accion_responsable FOREIGN KEY (responsable_id) REFERENCES usuario (id),
    CONSTRAINT fk_accion_usuario     FOREIGN KEY (usuario_id)     REFERENCES usuario (id)
);

CREATE TRIGGER trg_accion_no_update BEFORE UPDATE ON accion_seguridad
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'accion_seguridad es de solo agregar: UPDATE no permitido';
CREATE TRIGGER trg_accion_no_delete BEFORE DELETE ON accion_seguridad
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'accion_seguridad es de solo agregar: DELETE no permitido';

GRANT SELECT, INSERT ON cientifica.accion_seguridad TO 'biohub_app'@'%';