package co.biohub.cientifico.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * EJEMPLO para los otros tres: así se protege un endpoint por rol. Se borra cuando ya existan
 * los endpoints reales (HU-01, HU-18, ...). Sin token => 401; con token del rol equivocado => 403.
 */
@RestController
@RequestMapping("/api/ejemplo")
public class EjemploRolesController {

    @GetMapping("/investigador")
    @PreAuthorize("hasAnyRole('INVESTIGADOR','DIRECTOR')")
    public Map<String, String> soloInvestigacion(Authentication auth) {
        return Map.of("usuario", auth.getName(), "acceso", "investigador o director");
    }

    @GetMapping("/director")
    @PreAuthorize("hasRole('DIRECTOR')")
    public Map<String, String> soloDirector(Authentication auth) {
        return Map.of("usuario", auth.getName(), "acceso", "solo director");
    }
}
