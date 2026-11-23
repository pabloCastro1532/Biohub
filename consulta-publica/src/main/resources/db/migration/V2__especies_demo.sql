-- Nombres de las dos especies de la demo, con los mismos ids que el servicio científico (V2__datos_demo.sql).
-- Los eventos hallazgo.registrado solo traen el id de la especie; si llega una especie desconocida
-- el consumidor crea una fila provisional ("Especie N") para que el hallazgo no se pierda.
INSERT INTO especie_publica (id, nombre_cientifico, nombre_comun) VALUES
    (1, 'Panthera onca', 'Jaguar'),
    (2, 'Ara macao', 'Guacamaya bandera');
