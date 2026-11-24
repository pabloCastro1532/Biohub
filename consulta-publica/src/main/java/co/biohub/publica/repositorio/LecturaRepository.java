package co.biohub.publica.repositorio;

import co.biohub.publica.servicio.EspecieResumen;
import co.biohub.publica.servicio.HallazgoPublico;
import co.biohub.publica.servicio.HallazgoReciente;
import co.biohub.publica.servicio.RegionConteo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Acceso a la BD de lectura (esquema "lectura"). Aquí no existe ninguna columna de coordenadas. */
@Repository
public class LecturaRepository {

    public record EspecieBase(Long id, String nombreCientifico, String nombreComun) {}

    public record Agregado(long total, LocalDate ultima) {}

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {};

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    public LecturaRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // ---------------------------------------------------------------- lectura

    /** HU-18: especies que coinciden con el texto (nombre científico o común) y, si se pide, con una región. */
    public List<EspecieResumen> buscar(String patron, String region, int limite) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("limite", limite);
        StringBuilder sql = new StringBuilder("""
                SELECT e.id, e.nombre_cientifico, e.nombre_comun,
                       COUNT(h.hallazgo_id) AS total, MAX(h.fecha) AS ultima
                FROM especie_publica e
                LEFT JOIN hallazgo_publico h ON h.especie_id = e.id
                """);
        if (region != null) {
            sql.append(" AND h.region = :region\n");
            p.addValue("region", region);
        }
        if (patron != null) {
            sql.append(" WHERE (e.nombre_cientifico LIKE :patron OR e.nombre_comun LIKE :patron)\n");
            p.addValue("patron", patron);
        }
        sql.append(" GROUP BY e.id, e.nombre_cientifico, e.nombre_comun\n");
        if (region != null) {
            sql.append(" HAVING COUNT(h.hallazgo_id) > 0\n");
        }
        sql.append(" ORDER BY COALESCE(e.nombre_comun, e.nombre_cientifico), e.id\n LIMIT :limite");
        return jdbc.query(sql.toString(), p, (rs, n) -> new EspecieResumen(
                rs.getLong("id"), rs.getString("nombre_cientifico"), rs.getString("nombre_comun"),
                rs.getLong("total"), rs.getObject("ultima", LocalDate.class)));
    }

    public Optional<EspecieBase> especie(long id) {
        List<EspecieBase> filas = jdbc.query(
                "SELECT id, nombre_cientifico, nombre_comun FROM especie_publica WHERE id = :id",
                Map.of("id", id),
                (rs, n) -> new EspecieBase(rs.getLong("id"), rs.getString("nombre_cientifico"),
                        rs.getString("nombre_comun")));
        return filas.stream().findFirst();
    }

    public Agregado agregado(long especieId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) AS total, MAX(fecha) AS ultima FROM hallazgo_publico WHERE especie_id = :id",
                Map.of("id", especieId),
                (rs, n) -> new Agregado(rs.getLong("total"), rs.getObject("ultima", LocalDate.class)));
    }

    public List<RegionConteo> regiones(long especieId) {
        return jdbc.query("""
                        SELECT region, COUNT(*) AS total FROM hallazgo_publico
                        WHERE especie_id = :id GROUP BY region ORDER BY total DESC, region
                        """,
                Map.of("id", especieId),
                (rs, n) -> new RegionConteo(rs.getString("region"), rs.getLong("total")));
    }

    public List<HallazgoReciente> recientes(long especieId, int limite) {
        return jdbc.query("""
                        SELECT hallazgo_id, region, fecha, atributos FROM hallazgo_publico
                        WHERE especie_id = :id ORDER BY fecha DESC, hallazgo_id DESC LIMIT :limite
                        """,
                new MapSqlParameterSource().addValue("id", especieId).addValue("limite", limite),
                (rs, n) -> new HallazgoReciente(rs.getLong("hallazgo_id"), rs.getString("region"),
                        rs.getObject("fecha", LocalDate.class), leerAtributos(rs.getString("atributos"))));
    }

    // ------------------------------------------------------------- proyección

    /** Marca el evento como procesado. Devuelve false si ya se había procesado (idempotencia). */
    public boolean registrarEvento(String idEvento) {
        try {
            jdbc.update("INSERT INTO evento_procesado (id_evento) VALUES (:id)", Map.of("id", idEvento));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Si el evento trae una especie que no se conoce, se crea una fila provisional para no perder el hallazgo. */
    public void asegurarEspecie(long id) {
        jdbc.update("INSERT IGNORE INTO especie_publica (id, nombre_cientifico) VALUES (:id, :nombre)",
                Map.of("id", id, "nombre", "Especie " + id));
    }

    /**
     * Guarda el nombre de una especie nueva (evento especie.registrada). Si ya había una fila provisional ("Especie N",
     * porque un hallazgo llegó primero), le pone el nombre real. Repetirlo no cambia nada: es idempotente.
     */
    public void guardarEspecie(long id, String nombreCientifico, String nombreComun) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("cientifico", nombreCientifico)
                .addValue("comun", nombreComun);
        jdbc.update("INSERT INTO especie_publica (id, nombre_cientifico, nombre_comun) VALUES (:id, :cientifico, :comun)"
                + " ON DUPLICATE KEY UPDATE nombre_cientifico = VALUES(nombre_cientifico),"
                + " nombre_comun = VALUES(nombre_comun)", p);
    }

    public Optional<Integer> versionDe(long hallazgoId) {
        List<Integer> filas = jdbc.query("SELECT version FROM hallazgo_publico WHERE hallazgo_id = :id",
                Map.of("id", hallazgoId), (rs, n) -> rs.getInt("version"));
        return filas.stream().findFirst();
    }

    public void insertarHallazgo(HallazgoPublico h) {
        jdbc.update("""
                INSERT INTO hallazgo_publico (hallazgo_id, especie_id, version, region, fecha, atributos)
                VALUES (:hallazgo, :especie, :version, :region, :fecha, :atributos)
                """, parametros(h));
    }

    public void actualizarHallazgo(HallazgoPublico h) {
        jdbc.update("""
                UPDATE hallazgo_publico
                SET especie_id = :especie, version = :version, region = :region, fecha = :fecha,
                    atributos = :atributos, actualizado_en = CURRENT_TIMESTAMP(3)
                WHERE hallazgo_id = :hallazgo
                """, parametros(h));
    }

    private MapSqlParameterSource parametros(HallazgoPublico h) {
        return new MapSqlParameterSource()
                .addValue("hallazgo", h.hallazgoId())
                .addValue("especie", h.especieId())
                .addValue("version", h.version())
                .addValue("region", h.region())
                .addValue("fecha", h.fecha())
                .addValue("atributos", escribirAtributos(h.atributos()));
    }

    private String escribirAtributos(Map<String, Object> atributos) {
        try {
            return json.writeValueAsString(atributos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron guardar los atributos", e);
        }
    }

    private Map<String, Object> leerAtributos(String texto) {
        try {
            return texto == null ? Map.of() : json.readValue(texto, MAPA);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Atributos guardados con formato inválido", e);
        }
    }
}
