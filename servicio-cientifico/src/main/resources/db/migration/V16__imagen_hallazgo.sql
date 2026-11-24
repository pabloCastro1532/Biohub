-- Imagenes de un hallazgo. Se guardan en la base (sin carpetas aparte) y solo se agregan: no se editan ni se borran.
-- Cuelgan de la identidad del hallazgo, no de una version: reclasificar o editar no las toca.
CREATE TABLE imagen_hallazgo (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hallazgo_id     BIGINT       NOT NULL,
    nombre_archivo  VARCHAR(100) NOT NULL,
    tipo_contenido  VARCHAR(20)  NOT NULL,
    tamano          INT          NOT NULL,
    contenido       MEDIUMBLOB   NOT NULL,
    subida_por      BIGINT       NOT NULL,
    creada_en       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_imagen_hallazgo (hallazgo_id),
    CONSTRAINT fk_imagen_hallazgo FOREIGN KEY (hallazgo_id) REFERENCES hallazgo (id),
    CONSTRAINT fk_imagen_usuario  FOREIGN KEY (subida_por)  REFERENCES usuario (id)
);

CREATE TRIGGER trg_imagen_no_update BEFORE UPDATE ON imagen_hallazgo
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'imagen_hallazgo es de solo agregar: UPDATE no permitido';
CREATE TRIGGER trg_imagen_no_delete BEFORE DELETE ON imagen_hallazgo
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'imagen_hallazgo es de solo agregar: DELETE no permitido';

GRANT SELECT, INSERT ON cientifica.imagen_hallazgo TO 'biohub_app'@'%';
