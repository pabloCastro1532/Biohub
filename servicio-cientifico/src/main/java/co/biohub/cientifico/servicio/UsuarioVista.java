package co.biohub.cientifico.servicio;

import java.time.LocalDateTime;

/** Lo que se muestra de una cuenta: nunca incluye la clave ni su hash. */
public record UsuarioVista(Long id, String nombreUsuario, String rol, boolean activo, LocalDateTime creadoEn) {}
