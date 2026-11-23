package co.biohub.cientifico.repositorio;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioConsultaJdbc implements UsuarioConsulta {

    private static final String BASE = "SELECT id, nombre_usuario, rol, activo FROM usuario WHERE ";

    private static final RowMapper<UsuarioBasico> FILA = (rs, n) -> new UsuarioBasico(
            rs.getLong("id"), rs.getString("nombre_usuario"), rs.getString("rol"), rs.getBoolean("activo"));

    private final JdbcTemplate jdbc;

    public UsuarioConsultaJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UsuarioBasico> porNombre(String nombreUsuario) {
        return jdbc.query(BASE + "nombre_usuario = ?", FILA, nombreUsuario).stream().findFirst();
    }

    @Override
    public Optional<UsuarioBasico> porId(Long id) {
        return jdbc.query(BASE + "id = ?", FILA, id).stream().findFirst();
    }
}
