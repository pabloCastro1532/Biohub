package co.biohub.publica.servicio;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;

/** Un hallazgo reciente de la ficha: región, fecha y atributos públicos. Nunca trae coordenadas. */
public record HallazgoReciente(Long hallazgoId, String region, LocalDate fecha,
                               Map<String, Object> atributos) implements Serializable {}
