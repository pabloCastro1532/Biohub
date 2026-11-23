package co.biohub.cientifico.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Datos con los que un investigador registra un hallazgo (HU-01). */
public record NuevoHallazgo(Long especieId, String region, BigDecimal latitud, BigDecimal longitud,
                            LocalDate fechaObservacion, Map<String, Object> atributos) {}
