package co.biohub.cientifico.api;

import co.biohub.cientifico.servicio.EdicionHallazgo;
import co.biohub.cientifico.servicio.HallazgoService;
import co.biohub.cientifico.servicio.HallazgoVista;
import co.biohub.cientifico.servicio.NuevoHallazgo;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * HU-01: hallazgos. Registrar: investigador y director. Consultar: investigador, director y auditor.
 * HU-08: editar (crea una versión nueva con motivo): investigador y director.
 * Ninguna respuesta lleva la ubicación exacta (ASR-4); esa sale por su propio camino, con registro de acceso.
 */
@RestController
@RequestMapping("/api/hallazgos")
public class HallazgoController {

    public record HallazgoRequest(
            @NotNull Long especieId,
            @NotBlank @Size(max = 120) String region,
            @NotNull BigDecimal latitud,
            @NotNull BigDecimal longitud,
            @NotNull LocalDate fechaObservacion,
            Map<String, Object> atributos) {}

    /** El motivo se valida en el servicio para devolver un mensaje claro. */
    public record EdicionRequest(
            @NotBlank @Size(max = 120) String region,
            @NotNull BigDecimal latitud,
            @NotNull BigDecimal longitud,
            @NotNull LocalDate fechaObservacion,
            Map<String, Object> atributos,
            @Size(max = 500) String motivo) {}

    private final HallazgoService servicio;

    public HallazgoController(HallazgoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INVESTIGADOR','DIRECTOR')")
    public HallazgoVista registrar(@Valid @RequestBody HallazgoRequest req, Authentication usuario) {
        return servicio.registrar(new NuevoHallazgo(req.especieId(), req.region(), req.latitud(), req.longitud(),
                req.fechaObservacion(), req.atributos()), usuario.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INVESTIGADOR','DIRECTOR')")
    public HallazgoVista editar(@PathVariable Long id, @Valid @RequestBody EdicionRequest req, Authentication usuario) {
        return servicio.editar(id, new EdicionHallazgo(req.region(), req.latitud(), req.longitud(),
                req.fechaObservacion(), req.atributos(), req.motivo()), usuario.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INVESTIGADOR','DIRECTOR','AUDITOR')")
    public List<HallazgoVista> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('INVESTIGADOR','DIRECTOR','AUDITOR')")
    public HallazgoVista obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }
}
