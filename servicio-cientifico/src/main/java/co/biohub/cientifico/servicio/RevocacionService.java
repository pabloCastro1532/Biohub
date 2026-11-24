package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.TipoAccionSeguridad;
import co.biohub.cientifico.repositorio.AccionSeguridadJdbc;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-27 (ASR-4): el responsable de seguridad revoca la sesión y el rol de un usuario.
 * Deja constancia de quién lo hizo y desactiva al usuario; el filtro JWT hace el resto (401 en la siguiente petición).
 */
@Service
public class RevocacionService {

    private final UsuarioConsulta usuarios;
    private final AccionSeguridadJdbc acciones;

    public RevocacionService(UsuarioConsulta usuarios, AccionSeguridadJdbc acciones) {
        this.usuarios = usuarios;
        this.acciones = acciones;
    }

    @Transactional
    public void revocar(String responsable, Long usuarioId) {
        UsuarioBasico objetivo = usuarios.porId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + usuarioId));
        UsuarioBasico quien = usuarios.porNombre(responsable)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + responsable));
        acciones.registrar(TipoAccionSeguridad.REVOCACION, quien.id(), objetivo.id());
        acciones.desactivarUsuario(objetivo.id());
    }
}