package co.biohub.cientifico.servicio;

import java.time.LocalDateTime;

/** Un préstamo con el código del equipo ya resuelto, listo para mostrar. */
public record PrestamoVista(
        Long id,
        Long equipoId,
        String equipoCodigo,
        String grupo,
        String observacion,
        String prestadoPor,
        LocalDateTime prestadoEn,
        LocalDateTime devueltoEn,
        String devueltoPor) {
}
