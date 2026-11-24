package co.biohub.publica.servicio;

import java.io.Serializable;

/** Cuántos hallazgos de una especie hay en una región o departamento. */
public record RegionConteo(String region, long total) implements Serializable {}
