package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.ResultadoAcceso;

import java.time.LocalDateTime;

/** Evento interno de una consulta de ubicación exacta. Igual a contratos/acceso-coordenadas-registrado.schema.json: sin coordenadas. */
public record AccesoCoordenadasEvento(Long usuarioId, Long hallazgoId, ResultadoAcceso resultado,
                                      LocalDateTime ocurridoEn) {}
