package co.biohub.cientifico.api;

import co.biohub.cientifico.servicio.PrestamoService;
import co.biohub.cientifico.servicio.PrestamoVista;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HU-04 y HU-05: préstamo de equipos a grupos.
 * Prestar y devolver: administrador y director. Consultar: administrador, director e investigador.
 */
@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    public record PrestamoRequest(
            @NotNull Long equipoId,
            @NotBlank @Size(max = 120) String grupo,
            @Size(max = 400) String observacion) {}

    private final PrestamoService servicio;

    public PrestamoController(PrestamoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR')")
    public PrestamoVista prestar(@Valid @RequestBody PrestamoRequest req, Authentication usuario) {
        return servicio.prestar(req.equipoId(), req.grupo(), req.observacion(), usuario.getName());
    }

    @PostMapping("/{id}/devolucion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR')")
    public PrestamoVista devolver(@PathVariable Long id, Authentication usuario) {
        return servicio.devolver(id, usuario.getName());
    }

    /** Sin parámetros lista todos los préstamos; con {@code ?abiertos=true} solo los que siguen sin devolver. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','INVESTIGADOR')")
    public List<PrestamoVista> listar(@RequestParam(defaultValue = "false") boolean abiertos) {
        return servicio.listar(abiertos);
    }
}
