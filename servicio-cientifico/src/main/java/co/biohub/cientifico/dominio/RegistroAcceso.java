package co.biohub.cientifico.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Una consulta de ubicación exacta, aceptada o rechazada (HU-25, ASR-4).
 * La tabla es de solo agregar: el usuario de la aplicación solo tiene INSERT y SELECT, y hay triggers contra UPDATE y DELETE.
 * Aquí no se guarda ninguna coordenada: solo quién consultó, qué hallazgo y con qué resultado.
 */
@Entity
@Table(name = "registro_acceso")
public class RegistroAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "hallazgo_id", nullable = false)
    private Long hallazgoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoAcceso resultado;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;

    protected RegistroAcceso() {
        // requerido por JPA
    }

    public static RegistroAcceso nuevo(Long usuarioId, Long hallazgoId, ResultadoAcceso resultado) {
        RegistroAcceso r = new RegistroAcceso();
        r.usuarioId = usuarioId;
        r.hallazgoId = hallazgoId;
        r.resultado = resultado;
        r.ocurridoEn = LocalDateTime.now();
        return r;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getHallazgoId() { return hallazgoId; }
    public ResultadoAcceso getResultado() { return resultado; }
    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
}
