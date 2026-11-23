package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.Hallazgo;
import co.biohub.cientifico.dominio.VersionHallazgo;
import co.biohub.cientifico.repositorio.EspecieRepository;
import co.biohub.cientifico.repositorio.HallazgoRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import co.biohub.cientifico.repositorio.VersionHallazgoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HU-01: registrar un hallazgo con sus datos básicos. Crea la identidad del hallazgo y su versión 1.
 * Los atributos se validan contra la plantilla activa de la especie (HU-02).
 */
@Service
public class HallazgoService {

    private static final BigDecimal LATITUD_MAXIMA = new BigDecimal("90");
    private static final BigDecimal LONGITUD_MAXIMA = new BigDecimal("180");

    private final HallazgoRepository hallazgos;
    private final VersionHallazgoRepository versiones;
    private final EspecieRepository especies;
    private final PlantillaService plantillas;
    private final UsuarioConsulta usuarios;
    private final ApplicationEventPublisher eventos;

    public HallazgoService(HallazgoRepository hallazgos, VersionHallazgoRepository versiones,
                           EspecieRepository especies, PlantillaService plantillas,
                           UsuarioConsulta usuarios, ApplicationEventPublisher eventos) {
        this.hallazgos = hallazgos;
        this.versiones = versiones;
        this.especies = especies;
        this.plantillas = plantillas;
        this.usuarios = usuarios;
        this.eventos = eventos;
    }

    @Transactional
    public HallazgoVista registrar(NuevoHallazgo datos, String nombreAutor) {
        if (datos.especieId() == null || !especies.existsById(datos.especieId())) {
            throw new RecursoNoEncontradoException("No existe la especie " + datos.especieId());
        }
        String region = datos.region() == null ? "" : datos.region().trim();
        if (region.isEmpty()) {
            throw new SolicitudInvalidaException("La región es obligatoria");
        }
        if (region.length() > 120) {
            throw new SolicitudInvalidaException("La región no puede pasar de 120 caracteres");
        }
        BigDecimal latitud = coordenada(datos.latitud(), LATITUD_MAXIMA, "La latitud");
        BigDecimal longitud = coordenada(datos.longitud(), LONGITUD_MAXIMA, "La longitud");
        if (datos.fechaObservacion() == null) {
            throw new SolicitudInvalidaException("La fecha de observación es obligatoria");
        }
        if (datos.fechaObservacion().isAfter(LocalDate.now())) {
            throw new SolicitudInvalidaException("La fecha de observación no puede ser futura");
        }

        Map<String, Object> atributos = datos.atributos() == null ? new HashMap<>() : datos.atributos();
        List<String> errores = plantillas.validar(datos.especieId(), atributos);
        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException("Los atributos no cumplen la plantilla: " + String.join("; ", errores));
        }

        UsuarioBasico autor = usuarios.porNombre(nombreAutor)
                .orElseThrow(() -> new SolicitudInvalidaException("No se encontró el usuario " + nombreAutor));

        Hallazgo hallazgo = hallazgos.save(Hallazgo.nuevo(autor.id()));
        VersionHallazgo version = versiones.save(VersionHallazgo.nueva(
                hallazgo.getId(), 1, datos.especieId(), region, latitud, longitud,
                datos.fechaObservacion(), atributos, autor.id(), null));

        eventos.publishEvent(new HallazgoRegistradoEvento(
                hallazgo.getId(), datos.especieId(), version.getNumero(), region,
                datos.fechaObservacion(), atributos));

        return vista(hallazgo, version, autor.nombreUsuario());
    }

    @Transactional(readOnly = true)
    public HallazgoVista obtener(Long id) {
        Hallazgo hallazgo = hallazgos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el hallazgo " + id));
        return actual(hallazgo);
    }

    /** Todos los hallazgos con su versión más reciente, los nuevos primero. */
    @Transactional(readOnly = true)
    public List<HallazgoVista> listar() {
        return hallazgos.findAllByOrderByIdDesc().stream().map(this::actual).toList();
    }

    private HallazgoVista actual(Hallazgo hallazgo) {
        VersionHallazgo version = versiones.findFirstByHallazgoIdOrderByNumeroDesc(hallazgo.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El hallazgo " + hallazgo.getId() + " no tiene versiones"));
        String autor = usuarios.porId(version.getAutorId()).map(UsuarioBasico::nombreUsuario).orElse(null);
        return vista(hallazgo, version, autor);
    }

    private static HallazgoVista vista(Hallazgo hallazgo, VersionHallazgo v, String autor) {
        return new HallazgoVista(hallazgo.getId(), v.getNumero(), v.getEspecieId(), v.getRegion(),
                v.getFechaObservacion(), v.getAtributos(), autor, v.getCreadoEn());
    }

    /** Valida el rango y deja la coordenada con los 6 decimales que guarda la base de datos. */
    private static BigDecimal coordenada(BigDecimal valor, BigDecimal maximo, String nombre) {
        if (valor == null) {
            throw new SolicitudInvalidaException(nombre + " es obligatoria");
        }
        if (valor.abs().compareTo(maximo) > 0) {
            throw new SolicitudInvalidaException(nombre + " debe estar entre -" + maximo + " y " + maximo);
        }
        return valor.setScale(6, RoundingMode.HALF_UP);
    }
}
