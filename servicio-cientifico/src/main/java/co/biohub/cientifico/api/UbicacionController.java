package co.biohub.cientifico.api;

import co.biohub.cientifico.seguridad.Rol;
import co.biohub.cientifico.servicio.GuardiaUbicacion;
import co.biohub.cientifico.servicio.UbicacionVista;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-15, HU-16 y HU-17: ubicación de un hallazgo. Requiere sesión (sin token, 401). El director recibe
 * las coordenadas exactas; cualquier otro rol recibe solo la región. Toda la decisión la toma la guardia.
 */
@RestController
@RequestMapping("/api/hallazgos")
public class UbicacionController {

    private final GuardiaUbicacion guardia;

    public UbicacionController(GuardiaUbicacion guardia) {
        this.guardia = guardia;
    }

    @GetMapping("/{id}/ubicacion")
    @PreAuthorize("isAuthenticated()")
    public UbicacionVista ubicacion(@PathVariable Long id, Authentication usuario) {
        return guardia.consultar(id, usuario.getName(), rolDe(usuario));
    }

    private static Rol rolDe(Authentication autenticacion) {
        return autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> Rol.valueOf(a.substring("ROLE_".length())))
                .findFirst()
                .orElse(null);
    }
}