package co.biohub.cientifico.repositorio;

import java.util.Optional;

/**
 * Consulta de solo lectura sobre la tabla usuario. Se usa SQL directo para no depender de la entidad Usuario
 * de la autenticación (HU-22 y HU-23), que vive en otra rama.
 */
public interface UsuarioConsulta {

    record UsuarioBasico(Long id, String nombreUsuario, String rol, boolean activo) {}

    Optional<UsuarioBasico> porNombre(String nombreUsuario);

    Optional<UsuarioBasico> porId(Long id);
}
