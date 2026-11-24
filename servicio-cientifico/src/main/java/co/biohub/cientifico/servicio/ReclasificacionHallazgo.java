package co.biohub.cientifico.servicio;

import java.util.Map;

/**
 * Datos con los que se reclasifica un hallazgo (HU-09): la especie nueva, los atributos que pide la plantilla de esa
 * especie y el motivo. La región, la fecha y la ubicación se conservan de la versión actual.
 */
public record ReclasificacionHallazgo(Long especieId, Map<String, Object> atributos, String motivo) {}
