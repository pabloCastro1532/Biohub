package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.Especie;
import co.biohub.cientifico.dominio.PlantillaEspecie;
import co.biohub.cientifico.dominio.atributos.Atributo;
import co.biohub.cientifico.dominio.atributos.AtributoVista;
import co.biohub.cientifico.dominio.atributos.EsquemaAAtributos;
import co.biohub.cientifico.dominio.atributos.RegistroDeFabricas;
import co.biohub.cientifico.repositorio.EspecieRepository;
import co.biohub.cientifico.repositorio.PlantillaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * HU-02 / ASR-6: cada especie tiene una plantilla de atributos (JSON Schema) que se cambia sin tocar código.
 * Editar una plantilla crea una versión nueva y deja la anterior inactiva.
 */
@Service
public class PlantillaService {

    private final PlantillaRepository plantillas;
    private final EspecieRepository especies;
    private final ValidadorEsquema validador;
    private final RegistroDeFabricas registro;
    private final ObjectMapper mapper;

    public PlantillaService(PlantillaRepository plantillas, EspecieRepository especies,
                            ValidadorEsquema validador, RegistroDeFabricas registro, ObjectMapper mapper) {
        this.plantillas = plantillas;
        this.especies = especies;
        this.validador = validador;
        this.registro = registro;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<Especie> listarEspecies() {
        return especies.findAllByOrderByNombreCientificoAsc();
    }

    @Transactional(readOnly = true)
    public List<PlantillaEspecie> listarActivas() {
        return plantillas.findByActivaTrueOrderByEspecieIdAsc();
    }

    @Transactional(readOnly = true)
    public PlantillaEspecie obtener(Long id) {
        return plantillas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la plantilla " + id));
    }

    @Transactional(readOnly = true)
    public PlantillaEspecie activaDe(Long especieId) {
        return plantillas.findFirstByEspecieIdAndActivaTrueOrderByVersionDesc(especieId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La especie " + especieId + " no tiene plantilla activa"));
    }

    @Transactional
    public PlantillaEspecie crear(Long especieId, Map<String, Object> esquema) {
        if (!especies.existsById(especieId)) {
            throw new RecursoNoEncontradoException("No existe la especie " + especieId);
        }
        Map<String, Object> normalizado = normalizar(esquema);

        int siguiente = plantillas.findFirstByEspecieIdOrderByVersionDesc(especieId)
                .map(p -> p.getVersion() + 1)
                .orElse(1);
        plantillas.desactivarDe(especieId);
        return plantillas.save(PlantillaEspecie.nueva(especieId, siguiente, normalizado));
    }

    /** Editar = crear la versión siguiente de la plantilla de esa especie. */
    @Transactional
    public PlantillaEspecie editar(Long id, Map<String, Object> esquema) {
        PlantillaEspecie base = obtener(id);
        return crear(base.getEspecieId(), esquema);
    }

    /** Los atributos de la plantilla, listos para armar el formulario (usa el Factory Method). */
    public List<AtributoVista> atributosDe(PlantillaEspecie plantilla) {
        JsonNode esquema = mapper.valueToTree(plantilla.getEsquema());
        return EsquemaAAtributos.leer(esquema, registro).stream().map(Atributo::describir).toList();
    }

    /** Valida los atributos de un hallazgo contra la plantilla activa de su especie. Lista vacía = válido. */
    @Transactional(readOnly = true)
    public List<String> validar(Long especieId, Map<String, Object> atributos) {
        PlantillaEspecie plantilla = activaDe(especieId);
        JsonNode esquema = mapper.valueToTree(plantilla.getEsquema());
        JsonNode datos = mapper.valueToTree(atributos == null ? Map.of() : atributos);
        return validador.validar(esquema, datos);
    }

    private Map<String, Object> normalizar(Map<String, Object> esquema) {
        ObjectNode nodo = mapper.valueToTree(esquema);
        if (!"object".equals(nodo.path("type").asText())) {
            throw new SolicitudInvalidaException("La plantilla debe tener \"type\": \"object\"");
        }
        JsonNode propiedades = nodo.path("properties");
        if (!propiedades.isObject() || propiedades.isEmpty()) {
            throw new SolicitudInvalidaException("La plantilla debe declarar al menos un atributo en \"properties\"");
        }
        try {
            EsquemaAAtributos.leer(nodo, registro);
        } catch (IllegalArgumentException e) {
            throw new SolicitudInvalidaException(e.getMessage());
        }
        if (!nodo.has("additionalProperties")) {
            nodo.put("additionalProperties", false);   // no se aceptan atributos que la plantilla no declara
        }
        try {
            validador.validar(nodo, mapper.createObjectNode());
        } catch (RuntimeException e) {
            throw new SolicitudInvalidaException("La plantilla no es un JSON Schema válido: " + e.getMessage());
        }
        return mapper.convertValue(nodo, new TypeReference<Map<String, Object>>() { });
    }
}
