package co.biohub.cientifico.seguridad;

import co.biohub.cientifico.repositorio.CredencialesJdbc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Crea al arrancar un usuario demo por rol (investigador, director, ...) si no existe.
 * Se apaga con USUARIOS_DEMO=false. La clave se cambia con CLAVE_DEMO (por defecto "biohub").
 */
@Component
@ConditionalOnProperty(name = "biohub.usuarios-demo.habilitado", havingValue = "true", matchIfMissing = true)
public class UsuariosDemo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(UsuariosDemo.class);

    private final CredencialesJdbc credenciales;
    private final PasswordEncoder encoder;
    private final String clave;

    public UsuariosDemo(CredencialesJdbc credenciales, PasswordEncoder encoder,
                        @Value("${biohub.usuarios-demo.clave:biohub}") String clave) {
        this.credenciales = credenciales;
        this.encoder = encoder;
        this.clave = clave;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Rol rol : Rol.values()) {
            String nombre = rol.name().toLowerCase(Locale.ROOT);
            if (credenciales.porNombre(nombre).isPresent()) {
                continue;
            }
            if (credenciales.crearSiNoExiste(nombre, encoder.encode(clave), rol.name())) {
                LOG.info("Usuario demo creado: {} ({})", nombre, rol);
            }
        }
    }
}