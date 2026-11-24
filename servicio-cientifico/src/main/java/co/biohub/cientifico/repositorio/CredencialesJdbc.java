package co.biohub.cientifico.repositorio;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Credenciales para iniciar sesión (HU-22 y HU-23). Es la única clase que lee clave_hash:
 * el resto del sistema usa UsuarioConsulta, que nunca la expone.
 */
@Repository
public class CredencialesJdbc {

    public record Credencial(Long id, String nombreUsuario, String claveHash, String rol, boolean activo) {}

    private static final RowMapper<Credencial> FILA = (rs, n) -> new Credencial(
            rs.getLong("id"), rs.getString("nombre_usuario"), rs.getString("clave_hash"),
            rs.getString("rol"), rs.getBoolean("activo"));

    private final JdbcTemplate jdbc;

    public CredencialesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Credencial> porNombre(String nombreUsuario) {
        return jdbc.query("SELECT id, nombre_usuario, clave_hash, rol, activo FROM usuario WHERE nombre_usuario = ?",
                FILA, nombreUsuario).stream().findFirst();
    }

    /** Crea el usuario si no existe. Devuelve true si lo creó. */
    public boolean crearSiNoExiste(String nombreUsuario, String claveHash, String rol) {
        if (porNombre(nombreUsuario).isPresent()) {
            return false;
        }
        try {
            jdbc.update("INSERT INTO usuario (nombre_usuario, clave_hash, rol) VALUES (?, ?, ?)",
                    nombreUsuario, claveHash, rol);
            return true;
        } catch (DuplicateKeyException e) {
            return false;   // otra instancia lo creó al mismo tiempo
        }
    }
}