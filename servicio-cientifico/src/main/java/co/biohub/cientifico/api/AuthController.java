package co.biohub.cientifico.api;

import co.biohub.cientifico.servicio.AutenticacionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-22 y HU-23: login con usuario y clave guardados en la base de datos (BCrypt).
 * El contrato no cambió respecto al login falso: POST /api/auth/login -> {token, rol}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String usuario, @NotBlank String clave) {}

    public record LoginResponse(String token, String rol) {}

    private final AutenticacionService autenticacion;

    public AuthController(AutenticacionService autenticacion) {
        this.autenticacion = autenticacion;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return autenticacion.iniciarSesion(req.usuario(), req.clave())
                .map(s -> ResponseEntity.ok(new LoginResponse(s.token(), s.rol().name())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}