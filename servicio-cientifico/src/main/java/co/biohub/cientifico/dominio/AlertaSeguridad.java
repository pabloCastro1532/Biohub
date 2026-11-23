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

/** Una alerta del monitor de seguridad (HU-26). Tabla de solo agregar: nunca se modifica ni se borra. */
@Entity
@Table(name = "alerta_seguridad")
public class AlertaSeguridad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAlerta tipo;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "ventana_minutos", nullable = false)
    private int ventanaMinutos;

    @Column(name = "generada_en", nullable = false)
    private LocalDateTime generadaEn;

    @Column(nullable = false)
    private boolean enviada;

    protected AlertaSeguridad() {
        // requerido por JPA
    }

    public static AlertaSeguridad nueva(Long usuarioId, TipoAlerta tipo, int cantidad, int ventanaMinutos,
                                        LocalDateTime generadaEn, boolean enviada) {
        AlertaSeguridad a = new AlertaSeguridad();
        a.usuarioId = usuarioId;
        a.tipo = tipo;
        a.cantidad = cantidad;
        a.ventanaMinutos = ventanaMinutos;
        a.generadaEn = generadaEn;
        a.enviada = enviada;
        return a;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public TipoAlerta getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public int getVentanaMinutos() { return ventanaMinutos; }
    public LocalDateTime getGeneradaEn() { return generadaEn; }
    public boolean isEnviada() { return enviada; }
}
