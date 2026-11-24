package co.biohub.cientifico.repositorio;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Administración de cuentas: listar, crear y reactivar. Nunca lee clave_hash (eso es solo de CredencialesJdbc);
 * únicamente la escribe al crear la cuenta.
 */
@Repository
public class UsuarioAdminJdbc {

    public record Fila(Long id, String nombreUsuario, String rol, boolean activo, LocalDateTime creadoEn) {}

    private static final String COLUMNAS = "SELECT id, nombre_usuario, rol, activo, creado_en FROM usuario";

    private static final RowMapper<Fila> FILA = (rs, n) -> new Fila(
            rs.getLong("id"), rs.getString("nombre_usuario"), rs.getString("rol"),
            rs.getBoolean("activo"), rs.getObject("creado_en", LocalDateTime.class));

    private final JdbcTemplate jdbc;

    public UsuarioAdminJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Fila> listar() {
        return jdbc.query(COLUMNAS + " ORDER BY nombre_usuario", FILA);
    }

    /** Inserta la cuenta y la devuelve. Si el nombre ya existe lanza DuplicateKeyException. */
    public Fila crear(String nombreUsuario, String claveHash, String rol) {
        jdbc.update("INSERT INTO usuario (nombre_usuario, clave_hash, rol) VALUES (?, ?, ?)",
                nombreUsuario, claveHash, rol);
        return jdbc.query(COLUMNAS + " WHERE nombre_usuario = ?", FILA, nombreUsuario).get(0);
    }

    /** Vuelve a dar acceso a una cuenta revocada. */
    public void activar(Long usuarioId) {
        jdbc.update("UPDATE usuario SET activo = TRUE WHERE id = ?", usuarioId);
    }
}
