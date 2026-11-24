package co.biohub.cientifico.dominio;

/** Los dos patrones anómalos que vigila el monitor de seguridad (HU-26). */
public enum TipoAlerta {
    /** Más consultas de ubicación exacta de las normales en una hora. */
    CONSULTAS_EXCESIVAS,
    /** Varias consultas rechazadas seguidas en pocos minutos. */
    RECHAZOS_REPETIDOS
}
