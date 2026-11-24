package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.Especie;
import co.biohub.cientifico.repositorio.EspecieRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agregar una especie al catálogo. Nace con una plantilla inicial (un solo atributo opcional) para que de inmediato se
 * pueda registrar hallazgos de ella; la plantilla se ajusta después desde la pantalla de plantillas (HU-02).
 * Al confirmarse, se publica especie.registrada para que la consulta pública conozca el nombre.
 */
@Service
public class EspecieService {

    static final int LARGO_MAXIMO = 160;
    static final int LARGO_MINIMO = 3;

    private final EspecieRepository especies;
    private final PlantillaService plantillas;
    private final ApplicationEventPublisher eventos;

    public EspecieService(EspecieRepository especies, PlantillaService plantillas, ApplicationEventPublisher eventos) {
        this.especies = especies;
        this.plantillas = plantillas;
        this.eventos = eventos;
    }

    @Transactional
    public Especie crear(String nombreCientifico, String nombreComun) {
        String cientifico = nombreCientifico == null ? "" : nombreCientifico.trim();
        String comun = nombreComun == null || nombreComun.isBlank() ? null : nombreComun.trim();

        if (cientifico.isEmpty()) {
            throw new SolicitudInvalidaException("El nombre científico es obligatorio");
        }
        if (cientifico.length() < LARGO_MINIMO) {
            throw new SolicitudInvalidaException("El nombre científico debe tener al menos " + LARGO_MINIMO + " letras");
        }
        if (cientifico.length() > LARGO_MAXIMO) {
            throw new SolicitudInvalidaException("El nombre científico no puede pasar de " + LARGO_MAXIMO + " caracteres");
        }
        if (comun != null && comun.length() > LARGO_MAXIMO) {
            throw new SolicitudInvalidaException("El nombre común no puede pasar de " + LARGO_MAXIMO + " caracteres");
        }
        if (especies.existsByNombreCientificoIgnoreCase(cientifico)) {
            throw new ConflictoException("Ya existe una especie con el nombre científico " + cientifico);
        }

        Especie nueva;
        try {
            nueva = especies.saveAndFlush(new Especie(cientifico, comun));
        } catch (DataIntegrityViolationException e) {
            // dos registros simultáneos con el mismo nombre: la restricción UNIQUE de la base lo detiene
            throw new ConflictoException("Ya existe una especie con el nombre científico " + cientifico);
        }

        plantillas.crear(nueva.getId(), plantillaInicial());
        eventos.publishEvent(new EspecieRegistradaEvento(nueva.getId(), nueva.getNombreCientifico(), nueva.getNombreComun()));
        return nueva;
    }

    /** Un solo atributo opcional, para que la plantilla sea válida (exige al menos uno) sin imponer datos a la especie. */
    private static Map<String, Object> plantillaInicial() {
        Map<String, Object> observaciones = new LinkedHashMap<>();
        observaciones.put("type", "string");
        observaciones.put("title", "Observaciones");
        observaciones.put("description", "Anotaciones libres sobre el hallazgo.");

        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("observaciones", observaciones);

        Map<String, Object> esquema = new LinkedHashMap<>();
        esquema.put("type", "object");
        esquema.put("properties", propiedades);
        return esquema;
    }
}
