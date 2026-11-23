package co.biohub.cientifico.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Datos con los que se edita un hallazgo (HU-08). La especie no cambia aquí: eso es reclasificar (HU-09). */
public record EdicionHallazgo(String region, BigDecimal latitud, BigDecimal longitud,
                              LocalDate fechaObservacion, Map<String, Object> atributos, String motivo) {}
