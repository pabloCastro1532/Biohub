package co.biohub.cientifico.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Equipo de campo del Instituto (HU-03). El préstamo y la asignación a responsables llegan en HU-04, HU-05 y HU-06. */
@Entity
@Table(name = "equipo")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(length = 400)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo;

    /** La pone la base de datos (DEFAULT); Hibernate no la inserta ni la actualiza. */
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected Equipo() {
        // requerido por JPA
    }

    public static Equipo nuevo(String codigo, String nombre, String tipo, String descripcion) {
        Equipo e = new Equipo();
        e.codigo = codigo;
        e.nombre = nombre;
        e.tipo = tipo;
        e.descripcion = descripcion;
        e.activo = true;
        return e;
    }

    public Long getId() { return id; }

    public String getCodigo() { return codigo; }

    public String getNombre() { return nombre; }

    public String getTipo() { return tipo; }

    public String getDescripcion() { return descripcion; }

    public boolean isActivo() { return activo; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
