package co.biohub.cientifico.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Identidad estable de un hallazgo. Todo su contenido vive en {@link VersionHallazgo} (ASR-3). */
@Entity
@Table(name = "hallazgo")
public class Hallazgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    protected Hallazgo() {
        // requerido por JPA
    }

    public static Hallazgo nuevo(Long creadoPor) {
        Hallazgo h = new Hallazgo();
        h.creadoPor = creadoPor;
        h.creadoEn = LocalDateTime.now();
        return h;
    }

    public Long getId() { return id; }

    public Long getCreadoPor() { return creadoPor; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
