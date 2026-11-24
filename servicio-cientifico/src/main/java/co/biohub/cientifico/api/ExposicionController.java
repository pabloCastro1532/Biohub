package co.biohub.cientifico.api;

import co.biohub.cientifico.servicio.ExposicionService;
import co.biohub.cientifico.servicio.ExposicionVista;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-27: hallazgos cuyas coordenadas exactas consultó un usuario en un periodo (por defecto, los últimos 30 días).
 * Fechas en formato 2026-11-24T00:00:00. Solo el responsable de seguridad.
 */
@RestController
@RequestMapping("/api/seguridad/usuarios")
public class ExposicionController {

    private final ExposicionService servicio;

    public ExposicionController(ExposicionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}/exposicion")
    @PreAuthorize("hasRole('SEGURIDAD')")
    public List<ExposicionVista> exposicion(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            Authentication autenticacion) {
        return servicio.coordenadasExpuestas(autenticacion.getName(), id, desde, hasta);
    }
}