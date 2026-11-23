package co.biohub.cientifico.api;

import co.biohub.cientifico.servicio.ResponsableService;
import co.biohub.cientifico.servicio.ResponsableVista;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HU-06 y HU-07: responsable de un equipo.
 * Asignar: administrador y director. Consultar: administrador, director e investigador.
 */
@RestController
@RequestMapping("/api/equipos")
public class ResponsableController {

    public record AsignacionRequest(@NotBlank @Size(max = 80) String usuario) {}

    private final ResponsableService servicio;

    public ResponsableController(ResponsableService servicio) {
        this.servicio = servicio;
    }

    @PutMapping("/{equipoId}/responsable")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR')")
    public ResponsableVista asignar(@PathVariable Long equipoId, @Valid @RequestBody AsignacionRequest req,
                                    Authentication usuario) {
        return servicio.asignar(equipoId, req.usuario(), usuario.getName());
    }

    @GetMapping("/{equipoId}/responsable")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','INVESTIGADOR')")
    public ResponsableVista consultar(@PathVariable Long equipoId) {
        return servicio.consultar(equipoId);
    }

    /** Todos los equipos que hoy tienen responsable. */
    @GetMapping("/responsables")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','INVESTIGADOR')")
    public List<ResponsableVista> listar() {
        return servicio.listarVigentes();
    }
}
