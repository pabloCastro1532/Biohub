-- Esquema "lectura": copia pública de solo lectura. NO tiene columnas de coordenadas (ASR-4).
-- La alimenta el consumidor de eventos hallazgo.registrado; nadie escribe aquí por HTTP.

CREATE TABLE especie_publica (
    id                BIGINT       NOT NULL PRIMARY KEY,   -- el mismo id de la especie en el servicio científico
    nombre_cientifico VARCHAR(160) NOT NULL,
    nombre_comun      VARCHAR(160) NULL,
    INDEX idx_especie_nombre_comun (nombre_comun),
    INDEX idx_especie_nombre_cientifico (nombre_cientifico)
);

-- Última versión conocida de cada hallazgo, solo con lo público: región, fecha y atributos.
CREATE TABLE hallazgo_publico (
    hallazgo_id    BIGINT       NOT NULL PRIMARY KEY,
    especie_id     BIGINT       NOT NULL,
    version        INT          NOT NULL,
    region         VARCHAR(120) NOT NULL,
    fecha          DATE         NOT NULL,
    atributos      JSON         NOT NULL,
    actualizado_en DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_hallazgo_especie (especie_id, fecha),
    INDEX idx_hallazgo_region (region, especie_id),
    CONSTRAINT fk_hallazgo_especie FOREIGN KEY (especie_id) REFERENCES especie_publica (id)
);

-- Idempotencia: un evento que llega dos veces se procesa una sola.
CREATE TABLE evento_procesado (
    id_evento    VARCHAR(36) NOT NULL PRIMARY KEY,
    procesado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
