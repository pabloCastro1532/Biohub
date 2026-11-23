package co.biohub.cientifico.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Una versión del historial de un hallazgo (HU-10, HU-11, HU-12). Lleva quién la hizo, por qué y qué cambió
 * respecto a la anterior. A propósito NO lleva latitud ni longitud (ASR-4): solo dice que la "ubicación" cambió.
 */
public record VersionVista(int numero, Long especieId, String region, LocalDate fechaObservacion,
                           Map<String, Object> atributos, String autor, String motivo,
                           LocalDateTime creadoEn, List<String> cambios) {}
