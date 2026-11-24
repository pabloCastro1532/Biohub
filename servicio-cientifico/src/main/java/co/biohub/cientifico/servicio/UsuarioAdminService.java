package co.biohub.cientifico.servicio;

import co.biohub.cientifico.repositorio.UsuarioAdminJdbc;
import co.biohub.cientifico.repositorio.UsuarioAdminJdbc.Fila;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import co.biohub.cientifico.seguridad.Rol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Cuentas de usuario: el administrador las crea con su rol y puede devolver el acceso a una cuenta revocada (HU-27).
 * La clave se guarda siempre cifrada (BCrypt) y nunca sale en ninguna respuesta.
 */
@Service
public class UsuarioAdminService {

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioAdminService.class);

    static final int NOMBRE_MINIMO = 3;
    static final int NOMBRE_MAXIMO = 40;
    static final int CLAVE_MINIMA = 8;
    static final int CLAVE_MAXIMA_BYTES = 72;   // límite de BCrypt

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[a-z0-9][a-z0-9._-]*");

    private final UsuarioAdminJdbc cuentas;
    private final UsuarioConsulta usuarios;
    private final PasswordEncoder encoder;

    public UsuarioAdminService(UsuarioAdminJdbc cuentas, UsuarioConsulta usuarios, PasswordEncoder encoder) {
        this.cuentas = cuentas;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    public List<UsuarioVista> listar() {
        return cuentas.listar().stream().map(UsuarioAdminService::vista).toList();
    }

    public UsuarioVista crear(String nombreUsuario, String rolTexto, String clave, String administrador) {
        String nombre = nombreUsuario == null ? "" : nombreUsuario.trim().toLowerCase(Locale.ROOT);
        validarNombre(nombre);
        Rol rol = validarRol(rolTexto);
        validarClave(clave, nombre);

        if (usuarios.porNombre(nombre).isPresent()) {
            throw new ConflictoException("Ya existe un usuario llamado " + nombre);
        }
        Fila creada;
        try {
            creada = cuentas.crear(nombre, encoder.encode(clave), rol.name());
        } catch (DuplicateKeyException e) {
            // dos administradores creando el mismo nombre a la vez: la restricción UNIQUE de la base lo detiene
            throw new ConflictoException("Ya existe un usuario llamado " + nombre);
        }
        LOG.info("Cuenta creada: {} ({}) por {}", nombre, rol, administrador);
        return vista(creada);
    }

    public void reactivar(Long usuarioId, String administrador) {
        UsuarioBasico u = usuarios.porId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + usuarioId));
        if (u.activo()) {
            throw new ConflictoException("El usuario " + u.nombreUsuario() + " ya tiene acceso");
        }
        cuentas.activar(u.id());
        LOG.info("Acceso devuelto a {} por {}", u.nombreUsuario(), administrador);
    }

    private static void validarNombre(String nombre) {
        if (nombre.isEmpty()) {
            throw new SolicitudInvalidaException("El nombre de usuario es obligatorio");
        }
        if (nombre.length() < NOMBRE_MINIMO) {
            throw new SolicitudInvalidaException("El nombre de usuario debe tener al menos " + NOMBRE_MINIMO + " caracteres");
        }
        if (nombre.length() > NOMBRE_MAXIMO) {
            throw new SolicitudInvalidaException("El nombre de usuario no puede pasar de " + NOMBRE_MAXIMO + " caracteres");
        }
        if (!NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new SolicitudInvalidaException(
                    "El nombre de usuario solo puede tener letras sin tilde, números, punto, guion y guion bajo, y debe empezar con letra o número");
        }
    }

    private static Rol validarRol(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new SolicitudInvalidaException("Elige un rol");
        }
        try {
            return Rol.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SolicitudInvalidaException("El rol no es válido");
        }
    }

    private static void validarClave(String clave, String nombre) {
        if (clave == null || clave.isBlank()) {
            throw new SolicitudInvalidaException("La clave es obligatoria");
        }
        if (clave.length() < CLAVE_MINIMA) {
            throw new SolicitudInvalidaException("La clave debe tener al menos " + CLAVE_MINIMA + " caracteres");
        }
        if (clave.getBytes(StandardCharsets.UTF_8).length > CLAVE_MAXIMA_BYTES) {
            throw new SolicitudInvalidaException("La clave es demasiado larga (máximo " + CLAVE_MAXIMA_BYTES + " caracteres)");
        }
        if (clave.equalsIgnoreCase(nombre)) {
            throw new SolicitudInvalidaException("La clave no puede ser igual al nombre de usuario");
        }
    }

    private static UsuarioVista vista(Fila f) {
        return new UsuarioVista(f.id(), f.nombreUsuario(), f.rol(), f.activo(), f.creadoEn());
    }
}
