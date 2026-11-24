package co.biohub.cientifico.servicio;

import co.biohub.cientifico.repositorio.CredencialesJdbc;
import co.biohub.cientifico.repositorio.CredencialesJdbc.Credencial;
import co.biohub.cientifico.seguridad.JwtService;
import co.biohub.cientifico.seguridad.Rol;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * HU-22 y HU-23: inicia sesión con usuario y clave y emite el JWT con el rol (ASR-4).
 * Usuario inexistente, clave incorrecta o usuario inactivo dan el mismo resultado: vacío.
 */
@Service
public class AutenticacionService {

    public record SesionIniciada(String token, Rol rol) {}

    private final CredencialesJdbc credenciales;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String hashFalso;

    public AutenticacionService(CredencialesJdbc credenciales, PasswordEncoder encoder, JwtService jwt) {
        this.credenciales = credenciales;
        this.encoder = encoder;
        this.jwt = jwt;
        this.hashFalso = encoder.encode("no-es-una-clave-real");
    }

    public Optional<SesionIniciada> iniciarSesion(String usuario, String clave) {
        Optional<Credencial> encontrada = credenciales.porNombre(usuario);
        // Se compara siempre contra un hash: así tardar igual no revela si el usuario existe.
        String hash = encontrada.map(Credencial::claveHash).orElse(hashFalso);
        boolean claveCorrecta = encoder.matches(clave, hash);
        if (encontrada.isEmpty() || !claveCorrecta || !encontrada.get().activo()) {
            return Optional.empty();
        }
        Rol rol = Rol.valueOf(encontrada.get().rol());
        // El token lleva el nombre tal como está guardado, no como lo escribió quien inició sesión.
        return Optional.of(new SesionIniciada(jwt.emitir(encontrada.get().nombreUsuario(), rol), rol));
    }
}