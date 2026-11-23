package co.biohub.cientifico.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Plantilla de atributos de una especie (ASR-6), escrita en JSON Schema.
 * Editar una plantilla no la modifica: crea una versión nueva y deja la anterior inactiva.
 */
@Entity
@Table(name = "plantilla_especie")
public class PlantillaEspecie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "especie_id", nullable = false)
    private Long especieId;

    @Column(nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private Map<String, Object> esquema;

    @Column(nullable = false)
    private boolean activa;

    /** La pone la base de datos (DEFAULT); Hibernate no la inserta ni la actualiza. */
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected PlantillaEspecie() {
        // requerido por JPA
    }

    public static PlantillaEspecie nueva(Long especieId, int version, Map<String, Object> esquema) {
        PlantillaEspecie p = new PlantillaEspecie();
        p.especieId = especieId;
        p.version = version;
        p.esquema = esquema;
        p.activa = true;
        return p;
    }

    public Long getId() { return id; }

    public Long getEspecieId() { return especieId; }

    public int getVersion() { return version; }

    public Map<String, Object> getEsquema() { return esquema; }

    public boolean isActiva() { return activa; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
