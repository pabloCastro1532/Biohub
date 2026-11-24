package co.biohub.cientifico.servicio;

import java.time.LocalDateTime;

/** Datos de una imagen de un hallazgo, sin su contenido. El contenido se pide aparte. */
public record ImagenVista(Long id, String nombreArchivo, String tipoContenido, int tamano, String autor,
                          LocalDateTime creadaEn) {}
