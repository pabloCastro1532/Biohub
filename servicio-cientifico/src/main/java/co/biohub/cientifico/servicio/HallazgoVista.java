package co.biohub.cientifico.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/** Lo que se devuelve de un hallazgo. A propósito NO lleva latitud ni longitud (ASR-4). */
public record HallazgoVista(Long id, int version, Long especieId, String region, LocalDate fechaObservacion,
                            Map<String, Object> atributos, String autor, LocalDateTime creadoEn) {}
