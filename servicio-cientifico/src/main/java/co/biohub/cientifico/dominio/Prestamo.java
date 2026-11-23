package co.biohub.cientifico.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Préstamo de un equipo a un grupo (HU-04). Mientras {@code devueltoEn} sea nulo el equipo sigue prestado.
 * La columna equipo_abierto de la tabla es generada por la base de datos y no se mapea aquí.
 */
@Entity
@Table(name = "prestamo")
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipo_id", nullable = false)
    private Long equipoId;

    @Column(nullable = false, length = 120)
    private String grupo;

    @Column(length = 400)
    private String observacion;

    @Column(name = "prestado_por", nullable = false, length = 80)
    private String prestadoPor;

    @Column(name = "prestado_en", nullable = false)
    private LocalDateTime prestadoEn;

    /** Nace nulo; solo la consulta de devolución del repositorio la cambia, de forma atómica. */
    @Column(name = "devuelto_en")
    private LocalDateTime devueltoEn;

    @Column(name = "devuelto_por", length = 80)
    private String devueltoPor;

    protected Prestamo() {
        // requerido por JPA
    }

    public static Prestamo nuevo(Long equipoId, String grupo, String observacion, String prestadoPor) {
        Prestamo p = new Prestamo();
        p.equipoId = equipoId;
        p.grupo = grupo;
        p.observacion = observacion;
        p.prestadoPor = prestadoPor;
        p.prestadoEn = LocalDateTime.now();
        return p;
    }

    public Long getId() { return id; }

    public Long getEquipoId() { return equipoId; }

    public String getGrupo() { return grupo; }

    public String getObservacion() { return observacion; }

    public String getPrestadoPor() { return prestadoPor; }

    public LocalDateTime getPrestadoEn() { return prestadoEn; }

    public LocalDateTime getDevueltoEn() { return devueltoEn; }

    public String getDevueltoPor() { return devueltoPor; }

    public boolean estaAbierto() { return devueltoEn == null; }
}
