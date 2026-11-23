package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.Hallazgo;
import co.biohub.cientifico.dominio.VersionHallazgo;
import co.biohub.cientifico.repositorio.EspecieRepository;
import co.biohub.cientifico.repositorio.HallazgoRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import co.biohub.cientifico.repositorio.VersionHallazgoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HU-01: registrar un hallazgo (crea la identidad y su versión 1).
 * HU-08: editar un hallazgo (inserta la versión n+1 con un motivo obligatorio; la anterior no se toca, ASR-3).
 * Los atributos se validan contra la plantilla activa de la especie (HU-02).
 */
@Service
public class HallazgoService {

    private static final BigDecimal LATITUD_MAXIMA = new BigDecimal("90");
    private static final BigDecimal LONGITUD_MAXIMA = new BigDecimal("180");
    private static final int MOTIVO_MAXIMO = 500;

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
        Validados d = validar(datos.especieId(), datos.region(), datos.latitud(), datos.longitud(),
                datos.fechaObservacion(), datos.atributos());

        UsuarioBasico autor = autorDe(nombreAutor);

        Hallazgo hallazgo = hallazgos.save(Hallazgo.nuevo(autor.id()));
        VersionHallazgo version = versiones.save(VersionHallazgo.nueva(
                hallazgo.getId(), 1, datos.especieId(), d.region(), d.latitud(), d.longitud(),
                d.fecha(), d.atributos(), autor.id(), null));

        eventos.publishEvent(new HallazgoRegistradoEvento(
                hallazgo.getId(), datos.especieId(), version.getNumero(), d.region(), d.fecha(), d.atributos()));

        return vista(hallazgo, version, autor.nombreUsuario());
    }

    /**
     * HU-08: edita un hallazgo creando la versión siguiente. Exige un motivo, valida igual que al registrar y
     * rechaza una edición que no cambia nada. La especie se mantiene (cambiarla es HU-09).
     */
    @Transactional
    public HallazgoVista editar(Long id, EdicionHallazgo datos, String nombreAutor) {
        Hallazgo hallazgo = hallazgos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el hallazgo " + id));
        VersionHallazgo actual = versiones.findFirstByHallazgoIdOrderByNumeroDesc(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El hallazgo " + id + " no tiene versiones"));

        String motivo = datos.motivo() == null ? "" : datos.motivo().trim();
        if (motivo.isEmpty()) {
            throw new SolicitudInvalidaException("El motivo del cambio es obligatorio");
        }
        if (motivo.length() > MOTIVO_MAXIMO) {
            throw new SolicitudInvalidaException("El motivo no puede pasar de " + MOTIVO_MAXIMO + " caracteres");
        }

        Validados d = validar(actual.getEspecieId(), datos.region(), datos.latitud(), datos.longitud(),
                datos.fechaObservacion(), datos.atributos());
        if (sinCambios(actual, d)) {
            throw new SolicitudInvalidaException("No hay cambios respecto a la versión actual");
        }

        UsuarioBasico autor = autorDe(nombreAutor);

        VersionHallazgo nueva;
        try {
            nueva = versiones.save(VersionHallazgo.nueva(
                    id, actual.getNumero() + 1, actual.getEspecieId(), d.region(), d.latitud(), d.longitud(),
                    d.fecha(), d.atributos(), autor.id(), motivo));
        } catch (DataIntegrityViolationException e) {
            // Dos ediciones simultáneas sobre la misma versión: la restricción UNIQUE (hallazgo, número) deja pasar una.
            throw new ConflictoException("Otra persona editó este hallazgo al mismo tiempo; vuelve a cargarlo e inténtalo de nuevo");
        }

        eventos.publishEvent(new HallazgoRegistradoEvento(
                id, actual.getEspecieId(), nueva.getNumero(), d.region(), d.fecha(), d.atributos()));

        return vista(hallazgo, nueva, autor.nombreUsuario());
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

    private UsuarioBasico autorDe(String nombreAutor) {
        return usuarios.porNombre(nombreAutor)
                .orElseThrow(() -> new SolicitudInvalidaException("No se encontró el usuario " + nombreAutor));
    }

    private static HallazgoVista vista(Hallazgo hallazgo, VersionHallazgo v, String autor) {
        return new HallazgoVista(hallazgo.getId(), v.getNumero(), v.getEspecieId(), v.getRegion(),
                v.getFechaObservacion(), v.getAtributos(), autor, v.getCreadoEn());
    }

    /** Datos ya limpios y validados, listos para guardarse como una versión. */
    private record Validados(String region, BigDecimal latitud, BigDecimal longitud, LocalDate fecha,
                             Map<String, Object> atributos) {}

    /** Las mismas reglas para registrar y para editar. */
    private Validados validar(Long especieId, String regionEntrada, BigDecimal latitudEntrada,
                              BigDecimal longitudEntrada, LocalDate fecha, Map<String, Object> atributosEntrada) {
        String region = regionEntrada == null ? "" : regionEntrada.trim();
        if (region.isEmpty()) {
            throw new SolicitudInvalidaException("La región es obligatoria");
        }
        if (region.length() > 120) {
            throw new SolicitudInvalidaException("La región no puede pasar de 120 caracteres");
        }
        BigDecimal latitud = coordenada(latitudEntrada, LATITUD_MAXIMA, "La latitud");
        BigDecimal longitud = coordenada(longitudEntrada, LONGITUD_MAXIMA, "La longitud");
        if (fecha == null) {
            throw new SolicitudInvalidaException("La fecha de observación es obligatoria");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new SolicitudInvalidaException("La fecha de observación no puede ser futura");
        }

        Map<String, Object> atributos = atributosEntrada == null ? new HashMap<>() : atributosEntrada;
        List<String> errores = plantillas.validar(especieId, atributos);
        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException("Los atributos no cumplen la plantilla: " + String.join("; ", errores));
        }
        return new Validados(region, latitud, longitud, fecha, atributos);
    }

    private static boolean sinCambios(VersionHallazgo actual, Validados d) {
        return actual.getRegion().equals(d.region())
                && actual.getLatitud().compareTo(d.latitud()) == 0
                && actual.getLongitud().compareTo(d.longitud()) == 0
                && actual.getFechaObservacion().equals(d.fecha())
                && Objects.equals(actual.getAtributos(), d.atributos());
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
