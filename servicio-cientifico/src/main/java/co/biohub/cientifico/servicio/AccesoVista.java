package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.ResultadoAcceso;

import java.time.LocalDateTime;

/** Un acceso a coordenadas exactas, con el nombre del usuario ya resuelto. No incluye coordenadas (ASR-4). */
public record AccesoVista(Long id, Long usuarioId, String usuario, Long hallazgoId, ResultadoAcceso resultado,
                          LocalDateTime ocurridoEn) {}
