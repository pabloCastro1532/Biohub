package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.RegistroAcceso;
import co.biohub.cientifico.dominio.ResultadoAcceso;
import co.biohub.cientifico.repositorio.HallazgoRepository;
import co.biohub.cientifico.repositorio.RegistroAccesoRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HU-25: deja constancia de cada consulta de ubicación exacta, aceptada o rechazada (ASR-4).
 *
 * El endpoint que entrega las coordenadas (HU-16) debe llamar a {@link #registrar} en TODOS los caminos:
 * antes de responder si se acepta, y antes de negar si se rechaza.
 * Corre en su propia transacción: un rechazo (que suele terminar en excepción) no deshace el registro.
 */
@Service
public class RegistroAccesoService {

    private final RegistroAccesoRepository accesos;
    private final HallazgoRepository hallazgos;
    private final UsuarioConsulta usuarios;
    private final ApplicationEventPublisher eventos;

    public RegistroAccesoService(RegistroAccesoRepository accesos, HallazgoRepository hallazgos,
                                 UsuarioConsulta usuarios, ApplicationEventPublisher eventos) {
        this.accesos = accesos;
        this.hallazgos = hallazgos;
        this.usuarios = usuarios;
        this.eventos = eventos;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AccesoVista registrar(String nombreUsuario, Long hallazgoId, ResultadoAcceso resultado) {
        if (resultado == null) {
            throw new SolicitudInvalidaException("El resultado del acceso es obligatorio");
        }
        UsuarioBasico usuario = usuarios.porNombre(nombreUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + nombreUsuario));
        if (hallazgoId == null || !hallazgos.existsById(hallazgoId)) {
            throw new RecursoNoEncontradoException("No existe el hallazgo " + hallazgoId);
        }
        RegistroAcceso guardado = accesos.save(RegistroAcceso.nuevo(usuario.id(), hallazgoId, resultado));
        eventos.publishEvent(new AccesoCoordenadasEvento(usuario.id(), hallazgoId, resultado,
                guardado.getOcurridoEn()));
        return vista(guardado, usuario.nombreUsuario());
    }

    /** Los accesos más recientes (máximo 200), con filtros opcionales por usuario y por resultado. */
    @Transactional(readOnly = true)
    public List<AccesoVista> listar(Long usuarioId, ResultadoAcceso resultado) {
        List<RegistroAcceso> filas;
        if (usuarioId != null && resultado != null) {
            filas = accesos.findTop200ByUsuarioIdAndResultadoOrderByOcurridoEnDescIdDesc(usuarioId, resultado);
        } else if (usuarioId != null) {
            filas = accesos.findTop200ByUsuarioIdOrderByOcurridoEnDescIdDesc(usuarioId);
        } else if (resultado != null) {
            filas = accesos.findTop200ByResultadoOrderByOcurridoEnDescIdDesc(resultado);
        } else {
            filas = accesos.findTop200ByOrderByOcurridoEnDescIdDesc();
        }
        Map<Long, String> nombres = new HashMap<>();
        return filas.stream()
                .map(r -> vista(r, nombres.computeIfAbsent(r.getUsuarioId(), id ->
                        usuarios.porId(id).map(UsuarioBasico::nombreUsuario).orElse("usuario " + id))))
                .toList();
    }

    private static AccesoVista vista(RegistroAcceso r, String usuario) {
        return new AccesoVista(r.getId(), r.getUsuarioId(), usuario, r.getHallazgoId(), r.getResultado(),
                r.getOcurridoEn());
    }
}
