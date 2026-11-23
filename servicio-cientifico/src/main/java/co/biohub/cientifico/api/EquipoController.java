package co.biohub.cientifico.api;

import co.biohub.cientifico.dominio.Equipo;
import co.biohub.cientifico.servicio.EquipoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HU-03: equipos de campo.
 * Registrar: administrador y director. Consultar: administrador, director e investigador.
 */
@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    public record EquipoRequest(
            @NotBlank @Size(max = 40) String codigo,
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Size(max = 60) String tipo,
            @Size(max = 400) String descripcion) {}

    public record EquipoRespuesta(Long id, String codigo, String nombre, String tipo,
                                  String descripcion, boolean activo) {}

    private final EquipoService servicio;

    public EquipoController(EquipoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR')")
    public EquipoRespuesta registrar(@Valid @RequestBody EquipoRequest req) {
        return aRespuesta(servicio.registrar(req.codigo(), req.nombre(), req.tipo(), req.descripcion()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','INVESTIGADOR')")
    public List<EquipoRespuesta> listar() {
        return servicio.listar().stream().map(this::aRespuesta).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','INVESTIGADOR')")
    public EquipoRespuesta obtener(@PathVariable Long id) {
        return aRespuesta(servicio.obtener(id));
    }

    private EquipoRespuesta aRespuesta(Equipo e) {
        return new EquipoRespuesta(e.getId(), e.getCodigo(), e.getNombre(), e.getTipo(),
                e.getDescripcion(), e.isActivo());
    }
}
