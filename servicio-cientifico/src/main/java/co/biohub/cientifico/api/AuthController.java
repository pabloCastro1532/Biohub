package co.biohub.cientifico.api;

import co.biohub.cientifico.seguridad.JwtService;
import co.biohub.cientifico.seguridad.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * LOGIN FALSO del sábado: un usuario por rol, clave "biohub".
 * Yonnatan lo reemplaza el domingo (HU-22 y 23) por la tabla de usuarios con BCrypt; el contrato
 * de la API (POST /api/auth/login -> {token, rol}) no cambia, así que Angular no se entera.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String usuario, @NotBlank String clave) {}

    public record LoginResponse(String token, String rol) {}

    private static final Map<String, Rol> USUARIOS_DEMO = Map.of(
            "investigador", Rol.INVESTIGADOR,
            "director", Rol.DIRECTOR,
            "auditor", Rol.AUDITOR,
            "administrador", Rol.ADMINISTRADOR,
            "seguridad", Rol.SEGURIDAD,
            "visitante", Rol.VISITANTE);

    private final JwtService jwt;
    private final PasswordEncoder encoder;
    private final String claveDemoHash;

    public AuthController(JwtService jwt, PasswordEncoder encoder) {
        this.jwt = jwt;
        this.encoder = encoder;
        this.claveDemoHash = encoder.encode("biohub");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        Rol rol = USUARIOS_DEMO.get(req.usuario());
        if (rol == null || !encoder.matches(req.clave(), claveDemoHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new LoginResponse(jwt.emitir(req.usuario(), rol), rol.name()));
    }
}
