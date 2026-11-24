package co.biohub.cientifico.repositorio;

import co.biohub.cientifico.servicio.ContenidoImagen;
import co.biohub.cientifico.servicio.ImagenVista;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Imágenes de los hallazgos: tabla imagen_hallazgo, de solo agregar. Se usa SQL directo para no cargar el archivo al listar. */
@Repository
public class ImagenHallazgoJdbc {

    private final JdbcTemplate jdbc;

    public ImagenHallazgoJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int contar(Long hallazgoId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM imagen_hallazgo WHERE hallazgo_id = ?",
                Integer.class, hallazgoId);
        return n == null ? 0 : n;
    }

    public void guardar(Long hallazgoId, String nombreArchivo, String tipoContenido, byte[] contenido, Long subidaPor) {
        jdbc.update("""
                INSERT INTO imagen_hallazgo (hallazgo_id, nombre_archivo, tipo_contenido, tamano, contenido, subida_por, creada_en)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, hallazgoId, nombreArchivo, tipoContenido, contenido.length, contenido, subidaPor, LocalDateTime.now());
    }

    /** Las imágenes de un hallazgo, de la más antigua a la más nueva, sin el contenido. */
    public List<ImagenVista> listar(Long hallazgoId) {
        return jdbc.query("""
                SELECT i.id, i.nombre_archivo, i.tipo_contenido, i.tamano, u.nombre_usuario, i.creada_en
                FROM imagen_hallazgo i LEFT JOIN usuario u ON u.id = i.subida_por
                WHERE i.hallazgo_id = ?
                ORDER BY i.id
                """, (rs, n) -> new ImagenVista(rs.getLong("id"), rs.getString("nombre_archivo"),
                rs.getString("tipo_contenido"), rs.getInt("tamano"), rs.getString("nombre_usuario"),
                rs.getObject("creada_en", LocalDateTime.class)), hallazgoId);
    }

    /** El archivo de una imagen, solo si pertenece a ese hallazgo. */
    public Optional<ContenidoImagen> contenido(Long hallazgoId, Long imagenId) {
        return jdbc.query("SELECT tipo_contenido, contenido FROM imagen_hallazgo WHERE id = ? AND hallazgo_id = ?",
                (rs, n) -> new ContenidoImagen(rs.getString("tipo_contenido"), rs.getBytes("contenido")),
                imagenId, hallazgoId).stream().findFirst();
    }
}
