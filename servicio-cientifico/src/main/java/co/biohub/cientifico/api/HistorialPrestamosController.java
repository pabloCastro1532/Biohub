package co.biohub.cientifico.api;

import co.biohub.cientifico.servicio.HistorialPrestamosService;
import co.biohub.cientifico.servicio.PrestamoVista;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU-24: historial de préstamos de un equipo. Administrador, director e investigador pueden consultarlo. */
@RestController
@RequestMapping("/api/equipos")
public class HistorialPrestamosController {

    private final HistorialPrestamosService servicio;

    public HistorialPrestamosController(HistorialPrestamosService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}/prestamos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','INVESTIGADOR')")
    public List<PrestamoVista> historial(@PathVariable Long id) {
        return servicio.deEquipo(id);
    }
}
