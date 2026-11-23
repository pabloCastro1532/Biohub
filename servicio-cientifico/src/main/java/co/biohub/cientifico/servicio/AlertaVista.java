package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.TipoAlerta;

import java.time.LocalDateTime;

/** Una alerta del monitor de seguridad, con el nombre del usuario ya resuelto. */
public record AlertaVista(Long id, Long usuarioId, String usuario, TipoAlerta tipo, int cantidad,
                          int ventanaMinutos, LocalDateTime generadaEn, boolean enviada) {}
