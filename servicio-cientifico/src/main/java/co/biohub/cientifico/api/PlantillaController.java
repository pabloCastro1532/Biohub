package co.biohub.cientifico.api;

import co.biohub.cientifico.dominio.PlantillaEspecie;
import co.biohub.cientifico.dominio.atributos.AtributoVista;
import co.biohub.cientifico.servicio.PlantillaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * HU-02: plantillas de atributos por especie.
 * Leer: administrador, director e investigador (el formulario de hallazgos las necesita).
 * Crear o editar: administrador y director.
 */
@RestController
@RequestMapping("/api/plantillas")
public class PlantillaController {

    public record PlantillaRequest(@NotNull Long especieId, @NotNull Map<String, Object> esquema) {}

    public record EsquemaRequest(@NotNull Map<String, Object> esquema) {}

    public record PlantillaRespuesta(Long id, Long especieId, int version, boolean activa,
                                     Map<String, Object> esquema, List<AtributoVista> atributos) {}

    public record EspecieRespuesta(Long id, String nombreCientifico, String nombreComun) {}

    public record ValidacionRespuesta(boolean valido, List<String> errores) {}

    private final PlantillaService servicio;

    public PlantillaController(PlantillaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/especies")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','INVESTIGADOR')")
    public List<EspecieRespuesta> especies() {
        return servicio.listarEspecies().stream()
                .map(e -> new EspecieRespuesta(e.getId(), e.getNombreCientifico(), e.getNombreComun()))
                .toList();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','INVESTIGADOR')")
    public List<PlantillaRespuesta> listarActivas() {
        return servicio.listarActivas().stream().map(this::aRespuesta).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','INVESTIGADOR')")
    public PlantillaRespuesta obtener(@PathVariable Long id) {
        return aRespuesta(servicio.obtener(id));
    }

    @GetMapping("/activa/{especieId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','INVESTIGADOR')")
    public PlantillaRespuesta activaDe(@PathVariable Long especieId) {
        return aRespuesta(servicio.activaDe(especieId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR')")
    public PlantillaRespuesta crear(@Valid @RequestBody PlantillaRequest req) {
        return aRespuesta(servicio.crear(req.especieId(), req.esquema()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR')")
    public PlantillaRespuesta editar(@PathVariable Long id, @Valid @RequestBody EsquemaRequest req) {
        return aRespuesta(servicio.editar(id, req.esquema()));
    }

    /** Comprueba unos atributos contra la plantilla activa de la especie, sin guardar nada. */
    @PostMapping("/activa/{especieId}/validar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','INVESTIGADOR')")
    public ValidacionRespuesta validar(@PathVariable Long especieId, @RequestBody Map<String, Object> atributos) {
        List<String> errores = servicio.validar(especieId, atributos);
        return new ValidacionRespuesta(errores.isEmpty(), errores);
    }

    private PlantillaRespuesta aRespuesta(PlantillaEspecie p) {
        return new PlantillaRespuesta(p.getId(), p.getEspecieId(), p.getVersion(), p.isActiva(),
                p.getEsquema(), servicio.atributosDe(p));
    }
}
