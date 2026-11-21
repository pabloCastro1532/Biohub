-- Esquema "cientifica": fuente de verdad. La migra Flyway con root; la aplicación usa biohub_app.

CREATE TABLE especie (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre_cientifico VARCHAR(160) NOT NULL UNIQUE,
    nombre_comun     VARCHAR(160) NULL,
    creado_en        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

-- ASR-6: los atributos de cada especie los define una plantilla en JSON Schema (se edita sin tocar código).
CREATE TABLE plantilla_especie (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    especie_id  BIGINT      NOT NULL,
    version     INT         NOT NULL,
    esquema     JSON        NOT NULL,
    activa      BOOLEAN     NOT NULL DEFAULT TRUE,
    creado_en   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_plantilla_especie FOREIGN KEY (especie_id) REFERENCES especie (id),
    CONSTRAINT uq_plantilla UNIQUE (especie_id, version)
);

CREATE TABLE usuario (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre_usuario VARCHAR(80)  NOT NULL UNIQUE,
    clave_hash     VARCHAR(100) NOT NULL,
    rol            ENUM('INVESTIGADOR','DIRECTOR','AUDITOR','ADMINISTRADOR','SEGURIDAD','VISITANTE') NOT NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

-- Identidad estable de un hallazgo; todo el contenido vive en version_hallazgo.
CREATE TABLE hallazgo (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    creado_por BIGINT      NOT NULL,
    creado_en  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_hallazgo_usuario FOREIGN KEY (creado_por) REFERENCES usuario (id)
);

-- ASR-3: SOLO SE AGREGA. Editar un hallazgo = insertar la versión n+1 (HU-08), nunca modificar la anterior (HU-10).
CREATE TABLE version_hallazgo (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hallazgo_id       BIGINT        NOT NULL,
    numero            INT           NOT NULL,
    especie_id        BIGINT        NOT NULL,
    region            VARCHAR(120)  NOT NULL,
    latitud           DECIMAL(9,6)  NOT NULL,   -- ubicación exacta: solo sale por ProveedorUbicacion (ASR-4)
    longitud          DECIMAL(9,6)  NOT NULL,
    fecha_observacion DATE          NOT NULL,
    atributos         JSON          NOT NULL,   -- validados contra la plantilla de la especie
    autor_id          BIGINT        NOT NULL,
    motivo            VARCHAR(500)  NULL,       -- obligatorio al editar (HU-11); la versión 1 no lo necesita
    creado_en         DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_version_hallazgo FOREIGN KEY (hallazgo_id) REFERENCES hallazgo (id),
    CONSTRAINT fk_version_especie  FOREIGN KEY (especie_id)  REFERENCES especie (id),
    CONSTRAINT fk_version_autor    FOREIGN KEY (autor_id)    REFERENCES usuario (id),
    CONSTRAINT uq_version UNIQUE (hallazgo_id, numero)
);

-- ASR-4 / HU-25: cada consulta de ubicación exacta, aceptada o rechazada. También solo de agregar.
CREATE TABLE registro_acceso (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL,
    hallazgo_id BIGINT      NOT NULL,
    resultado   ENUM('ACEPTADO','RECHAZADO') NOT NULL,
    ocurrido_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_acceso_usuario_tiempo (usuario_id, ocurrido_en),
    CONSTRAINT fk_acceso_usuario  FOREIGN KEY (usuario_id)  REFERENCES usuario (id),
    CONSTRAINT fk_acceso_hallazgo FOREIGN KEY (hallazgo_id) REFERENCES hallazgo (id)
);

-- HU-27: sesiones revocadas (el filtro JWT responde 401 si el jti está aquí).
CREATE TABLE sesion_revocada (
    jti         VARCHAR(64) NOT NULL PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL,
    revocada_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_revocada_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

-- Triggers de inmutabilidad (segunda barrera, además de los permisos).
CREATE TRIGGER trg_version_no_update BEFORE UPDATE ON version_hallazgo
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'version_hallazgo es de solo agregar: UPDATE no permitido';
CREATE TRIGGER trg_version_no_delete BEFORE DELETE ON version_hallazgo
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'version_hallazgo es de solo agregar: DELETE no permitido';
CREATE TRIGGER trg_acceso_no_update BEFORE UPDATE ON registro_acceso
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'registro_acceso es de solo agregar: UPDATE no permitido';
CREATE TRIGGER trg_acceso_no_delete BEFORE DELETE ON registro_acceso
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'registro_acceso es de solo agregar: DELETE no permitido';

-- Permisos mínimos del usuario de la aplicación (requiere que 00_esquemas.sql ya haya creado biohub_app).
GRANT SELECT, INSERT ON cientifica.version_hallazgo TO 'biohub_app'@'%';   -- ASR-3: sin UPDATE ni DELETE
GRANT SELECT, INSERT ON cientifica.registro_acceso  TO 'biohub_app'@'%';   -- ASR-4: solo agregar
GRANT SELECT, INSERT ON cientifica.hallazgo         TO 'biohub_app'@'%';
GRANT SELECT, INSERT, UPDATE ON cientifica.especie           TO 'biohub_app'@'%';
GRANT SELECT, INSERT, UPDATE ON cientifica.plantilla_especie TO 'biohub_app'@'%';
GRANT SELECT, INSERT, UPDATE ON cientifica.usuario           TO 'biohub_app'@'%';
GRANT SELECT, INSERT, DELETE ON cientifica.sesion_revocada   TO 'biohub_app'@'%';
