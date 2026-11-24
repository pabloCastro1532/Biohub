package co.biohub.cientifico.api;

import co.biohub.cientifico.servicio.RevocacionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HU-27: revocar la sesión y el rol de un usuario. Solo el responsable de seguridad. */
@RestController
@RequestMapping("/api/seguridad/usuarios")
public class RevocacionController {

    private final RevocacionService servicio;

    public RevocacionController(RevocacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/{id}/revocar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SEGURIDAD')")
    public void revocar(@PathVariable Long id, Authentication autenticacion) {
        servicio.revocar(autenticacion.getName(), id);
    }
}