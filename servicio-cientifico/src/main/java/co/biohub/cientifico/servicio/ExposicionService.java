package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.RegistroAcceso;
import co.biohub.cientifico.dominio.ResultadoAcceso;
import co.biohub.cientifico.dominio.TipoAccionSeguridad;
import co.biohub.cientifico.repositorio.AccionSeguridadJdbc;
import co.biohub.cientifico.repositorio.RegistroAccesoRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HU-27: qué coordenadas exactas vio un usuario en un periodo. Solo cuentan los accesos aceptados (en los
 * rechazados no salió ninguna coordenada). La consulta del responsable también queda registrada.
 */
@Service
public class ExposicionService {

    private static final int DIAS_POR_DEFECTO = 30;

    private final RegistroAccesoRepository accesos;
    private final UsuarioConsulta usuarios;
    private final AccionSeguridadJdbc acciones;

    public ExposicionService(RegistroAccesoRepository accesos, UsuarioConsulta usuarios, AccionSeguridadJdbc acciones) {
        this.accesos = accesos;
        this.usuarios = usuarios;
        this.acciones = acciones;
    }

    @Transactional
    public List<ExposicionVista> coordenadasExpuestas(String responsable, Long usuarioId,
                                                      LocalDateTime desde, LocalDateTime hasta) {
        LocalDateTime fin = hasta != null ? hasta : LocalDateTime.now();
        LocalDateTime inicio = desde != null ? desde : fin.minusDays(DIAS_POR_DEFECTO);
        if (inicio.isAfter(fin)) {
            throw new SolicitudInvalidaException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        UsuarioBasico objetivo = usuarios.porId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + usuarioId));
        UsuarioBasico quien = usuarios.porNombre(responsable)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + responsable));

        acciones.registrar(TipoAccionSeguridad.CONSULTA_EXPOSICION, quien.id(), objetivo.id());

        // Vienen de la más reciente a la más antigua: la primera de cada hallazgo es su última consulta.
        return accesos.findByUsuarioIdAndResultadoAndOcurridoEnBetweenOrderByOcurridoEnDesc(
                        objetivo.id(), ResultadoAcceso.ACEPTADO, inicio, fin)
                .stream()
                .collect(Collectors.groupingBy(RegistroAcceso::getHallazgoId, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new ExposicionVista(e.getKey(), e.getValue().size(), e.getValue().get(0).getOcurridoEn()))
                .toList();
    }
}