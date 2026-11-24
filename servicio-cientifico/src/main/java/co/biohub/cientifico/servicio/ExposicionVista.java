package co.biohub.cientifico.servicio;

import java.time.LocalDateTime;

/** Un hallazgo cuyas coordenadas exactas vio un usuario: cuántas veces y cuándo fue la última. Sin coordenadas. */
public record ExposicionVista(Long hallazgoId, long consultas, LocalDateTime ultimaConsulta) {}