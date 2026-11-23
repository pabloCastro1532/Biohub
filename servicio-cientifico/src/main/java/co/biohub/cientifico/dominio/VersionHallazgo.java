package co.biohub.cientifico.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Una versión de un hallazgo. ASR-3: solo se agrega. Editar un hallazgo inserta la versión n+1 y nunca modifica
 * ni borra la anterior (la base de datos lo impide con triggers y con permisos). Esta clase no tiene setters.
 */
@Entity
@Table(name = "version_hallazgo")
public class VersionHallazgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hallazgo_id", nullable = false)
    private Long hallazgoId;

    @Column(nullable = false)
    private int numero;

    @Column(name = "especie_id", nullable = false)
    private Long especieId;

    @Column(nullable = false, length = 120)
    private String region;

    /** Ubicación exacta (ASR-4): nunca sale en las respuestas ordinarias ni en los eventos. */
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(name = "fecha_observacion", nullable = false)
    private LocalDate fechaObservacion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private Map<String, Object> atributos;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Column(length = 500)
    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    protected VersionHallazgo() {
        // requerido por JPA
    }

    public static VersionHallazgo nueva(Long hallazgoId, int numero, Long especieId, String region,
                                        BigDecimal latitud, BigDecimal longitud, LocalDate fechaObservacion,
                                        Map<String, Object> atributos, Long autorId, String motivo) {
        VersionHallazgo v = new VersionHallazgo();
        v.hallazgoId = hallazgoId;
        v.numero = numero;
        v.especieId = especieId;
        v.region = region;
        v.latitud = latitud;
        v.longitud = longitud;
        v.fechaObservacion = fechaObservacion;
        v.atributos = atributos;
        v.autorId = autorId;
        v.motivo = motivo;
        v.creadoEn = LocalDateTime.now();
        return v;
    }

    public Long getId() { return id; }

    public Long getHallazgoId() { return hallazgoId; }

    public int getNumero() { return numero; }

    public Long getEspecieId() { return especieId; }

    public String getRegion() { return region; }

    public BigDecimal getLatitud() { return latitud; }

    public BigDecimal getLongitud() { return longitud; }

    public LocalDate getFechaObservacion() { return fechaObservacion; }

    public Map<String, Object> getAtributos() { return atributos; }

    public Long getAutorId() { return autorId; }

    public String getMotivo() { return motivo; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
