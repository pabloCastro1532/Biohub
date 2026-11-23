package co.biohub.cientifico.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Especie del catálogo. Solo lectura desde este servicio (los datos demo los crea la migración V2). */
@Entity
@Table(name = "especie")
public class Especie {

    @Id
    private Long id;

    @Column(name = "nombre_cientifico", nullable = false)
    private String nombreCientifico;

    @Column(name = "nombre_comun")
    private String nombreComun;

    protected Especie() {
        // requerido por JPA
    }

    public Long getId() { return id; }

    public String getNombreCientifico() { return nombreCientifico; }

    public String getNombreComun() { return nombreComun; }
}
