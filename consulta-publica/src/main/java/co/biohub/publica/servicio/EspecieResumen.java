package co.biohub.publica.servicio;

import java.io.Serializable;
import java.time.LocalDate;

/** Una especie en los resultados de búsqueda (HU-18). Sin coordenadas: solo cuántos hallazgos tiene y cuándo fue el último. */
public record EspecieResumen(Long id, String nombreCientifico, String nombreComun, long totalHallazgos,
                             LocalDate ultimaObservacion) implements Serializable {}
