package co.biohub.cientifico.servicio;

import java.time.LocalDateTime;

/** Responsable vigente de un equipo. Si el equipo no tiene responsable, los tres últimos campos son nulos. */
public record ResponsableVista(Long equipoId, String equipoCodigo, String responsable,
                               String asignadoPor, LocalDateTime asignadoEn) {}
