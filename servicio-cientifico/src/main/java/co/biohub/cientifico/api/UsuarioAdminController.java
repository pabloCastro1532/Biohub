package co.biohub.cientifico.api;

import co.biohub.cientifico.servicio.UsuarioAdminService;
import co.biohub.cientifico.servicio.UsuarioVista;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cuentas de usuario: solo el administrador. Las reglas (nombre, rol, clave, sin repetir) las valida el servicio
 * y salen como 400 o 409 con un mensaje claro. Ninguna respuesta incluye la clave.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioAdminController {

    public record CrearUsuarioRequest(String nombreUsuario, String rol, String clave) {}

    private final UsuarioAdminService servicio;

    public UsuarioAdminController(UsuarioAdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<UsuarioVista> listar() {
        return servicio.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UsuarioVista crear(@RequestBody CrearUsuarioRequest req, Authentication autenticacion) {
        return servicio.crear(req.nombreUsuario(), req.rol(), req.clave(), autenticacion.getName());
    }

    @PostMapping("/{id}/reactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void reactivar(@PathVariable Long id, Authentication autenticacion) {
        servicio.reactivar(id, autenticacion.getName());
    }
}
