package co.biohub.cientifico.repositorio;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/** Acciones del responsable de seguridad (HU-27): tabla accion_seguridad, de solo agregar. */
@Repository
public class AccionSeguridadJdbc {

    private final JdbcTemplate jdbc;

    public AccionSeguridadJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Momento de la última revocación de ese usuario, si la hay. */
    public Optional<LocalDateTime> ultimaRevocacion(String nombreUsuario) {
        return jdbc.query("""
                SELECT MAX(a.ocurrida_en)
                FROM accion_seguridad a JOIN usuario u ON u.id = a.usuario_id
                WHERE u.nombre_usuario = ? AND a.tipo = 'REVOCACION'
                """, (rs, n) -> rs.getObject(1, LocalDateTime.class), nombreUsuario)
                .stream().filter(Objects::nonNull).findFirst();
    }
}