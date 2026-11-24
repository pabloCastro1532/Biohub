package co.biohub.cientifico.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Especie del catálogo. Las dos de la demo las crea la migración V2; las demás se agregan con POST /api/especies. */
@Entity
@Table(name = "especie")
public class Especie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_cientifico", nullable = false)
    private String nombreCientifico;

    @Column(name = "nombre_comun")
    private String nombreComun;

    protected Especie() {
        // requerido por JPA
    }

    public Especie(String nombreCientifico, String nombreComun) {
        this.nombreCientifico = nombreCientifico;
        this.nombreComun = nombreComun;
    }

    public Long getId() { return id; }

    public String getNombreCientifico() { return nombreCientifico; }

    public String getNombreComun() { return nombreComun; }
}
