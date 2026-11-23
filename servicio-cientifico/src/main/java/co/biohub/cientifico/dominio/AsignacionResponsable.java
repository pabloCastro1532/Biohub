package co.biohub.cientifico.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Responsable de un equipo (HU-06). Mientras {@code hasta} sea nulo la asignación está vigente.
 * La columna equipo_vigente de la tabla es generada por la base de datos y no se mapea aquí.
 */
@Entity
@Table(name = "asignacion_responsable")
public class AsignacionResponsable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipo_id", nullable = false)
    private Long equipoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "asignado_por", nullable = false, length = 80)
    private String asignadoPor;

    @Column(name = "asignado_en", nullable = false)
    private LocalDateTime asignadoEn;

    /** Nace nulo; solo la consulta de cierre del repositorio la cambia. */
    @Column(name = "hasta")
    private LocalDateTime hasta;

    protected AsignacionResponsable() {
        // requerido por JPA
    }

    public static AsignacionResponsable nueva(Long equipoId, Long usuarioId, String asignadoPor, LocalDateTime cuando) {
        AsignacionResponsable a = new AsignacionResponsable();
        a.equipoId = equipoId;
        a.usuarioId = usuarioId;
        a.asignadoPor = asignadoPor;
        a.asignadoEn = cuando;
        return a;
    }

    public Long getId() { return id; }

    public Long getEquipoId() { return equipoId; }

    public Long getUsuarioId() { return usuarioId; }

    public String getAsignadoPor() { return asignadoPor; }

    public LocalDateTime getAsignadoEn() { return asignadoEn; }

    public LocalDateTime getHasta() { return hasta; }
}
