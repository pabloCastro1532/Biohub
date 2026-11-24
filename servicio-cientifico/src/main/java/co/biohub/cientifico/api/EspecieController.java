package co.biohub.cientifico.api;

import co.biohub.cientifico.dominio.Especie;
import co.biohub.cientifico.servicio.EspecieService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agregar una especie al catálogo: administrador y director. La lista de especies se lee en GET /api/plantillas/especies.
 * Las reglas (nombre obligatorio, sin repetir) las valida el servicio y salen como 400 o 409 con un mensaje claro.
 */
@RestController
@RequestMapping("/api/especies")
public class EspecieController {

    public record EspecieRequest(String nombreCientifico, String nombreComun) {}

    public record EspecieRespuesta(Long id, String nombreCientifico, String nombreComun) {}

    private final EspecieService servicio;

    public EspecieController(EspecieService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR')")
    public EspecieRespuesta crear(@RequestBody EspecieRequest req) {
        Especie e = servicio.crear(req.nombreCientifico(), req.nombreComun());
        return new EspecieRespuesta(e.getId(), e.getNombreCientifico(), e.getNombreComun());
    }
}
