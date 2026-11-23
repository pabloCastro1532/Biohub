package co.biohub.publica.servicio;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/** Ficha pública de una especie (HU-19). */
public record FichaEspecie(Long id, String nombreCientifico, String nombreComun, long totalHallazgos,
                           LocalDate ultimaObservacion, List<RegionConteo> regiones,
                           List<HallazgoReciente> recientes) implements Serializable {}
