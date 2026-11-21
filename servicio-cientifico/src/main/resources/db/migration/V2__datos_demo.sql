-- Datos mínimos para la demo. Dos especies con plantilla distinta (ASR-6: la especie B recibirá un atributo nuevo en vivo).

INSERT INTO especie (nombre_cientifico, nombre_comun) VALUES
    ('Panthera onca', 'Jaguar'),
    ('Ara macao', 'Guacamaya bandera');

INSERT INTO plantilla_especie (especie_id, version, esquema) VALUES
    ((SELECT id FROM especie WHERE nombre_cientifico = 'Panthera onca'), 1, JSON_OBJECT(
        'type', 'object',
        'properties', JSON_OBJECT(
            'peso_kg',      JSON_OBJECT('type', 'number', 'minimum', 0),
            'sexo',         JSON_OBJECT('type', 'string', 'enum', JSON_ARRAY('macho', 'hembra', 'desconocido')),
            'huella_cm',    JSON_OBJECT('type', 'number', 'minimum', 0)
        ),
        'required', JSON_ARRAY('sexo')
    )),
    ((SELECT id FROM especie WHERE nombre_cientifico = 'Ara macao'), 1, JSON_OBJECT(
        'type', 'object',
        'properties', JSON_OBJECT(
            'envergadura_cm', JSON_OBJECT('type', 'number', 'minimum', 0),
            'en_bandada',     JSON_OBJECT('type', 'boolean')
        ),
        'required', JSON_ARRAY('en_bandada')
    ));
