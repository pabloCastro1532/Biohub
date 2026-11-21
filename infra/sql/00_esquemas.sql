-- Se ejecuta UNA vez, al crear el volumen de MySQL (usuario root).
-- Esquemas y usuarios. Los permisos sobre TABLAS se otorgan en las migraciones Flyway,
-- porque las tablas todavía no existen aquí.

CREATE DATABASE IF NOT EXISTS cientifica CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS lectura    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;  -- sin coordenadas
CREATE DATABASE IF NOT EXISTS analitica  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;  -- réplica (nivel 3)
CREATE DATABASE IF NOT EXISTS reportes   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;  -- nivel 3

-- Un usuario por servicio, con permisos mínimos.
-- Servicio científico: sus permisos exactos (INSERT y SELECT sobre versiones) los da la migración V1.
CREATE USER IF NOT EXISTS 'biohub_app'@'%'      IDENTIFIED BY 'biohub_app';

-- Consulta pública: dueña del esquema lectura (que nunca tiene coordenadas) y sin acceso a cientifica.
CREATE USER IF NOT EXISTS 'biohub_publica'@'%'  IDENTIFIED BY 'biohub_publica';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON lectura.* TO 'biohub_publica'@'%';

-- Procesador de reportes (nivel 3): lee la réplica, escribe en reportes.
CREATE USER IF NOT EXISTS 'biohub_reportes'@'%' IDENTIFIED BY 'biohub_reportes';
GRANT SELECT ON analitica.* TO 'biohub_reportes'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON reportes.* TO 'biohub_reportes'@'%';

FLUSH PRIVILEGES;
