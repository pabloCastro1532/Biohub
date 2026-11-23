package co.biohub.publica.servicio;

import co.biohub.publica.repositorio.LecturaRepository;
import co.biohub.publica.repositorio.LecturaRepository.Agregado;
import co.biohub.publica.repositorio.LecturaRepository.EspecieBase;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * HU-18 (búsqueda) y HU-19 (ficha). Solo lee la BD de lectura, nunca la científica, y las respuestas se guardan en
 * Redis por poco tiempo. Si un evento cambia una especie, el consumidor invalida su ficha y las búsquedas.
 */
@Service
public class ConsultaEspecies {

    static final int HALLAZGOS_RECIENTES = 5;

    private final LecturaRepository lectura;

    public ConsultaEspecies(LecturaRepository lectura) {
        this.lectura = lectura;
    }

    /** Los parámetros llegan ya limpios (ver ParametrosBusqueda), así dos formas de pedir lo mismo comparten caché. */
    @Cacheable(value = "busquedas", key = "#patron + '|' + #region + '|' + #limite")
    public List<EspecieResumen> buscar(String patron, String region, int limite) {
        return lectura.buscar(patron, region, limite);
    }

    @Cacheable(value = "fichas", key = "#id")
    public FichaEspecie ficha(long id) {
        EspecieBase especie = lectura.especie(id).orElseThrow(() -> new EspecieNoEncontradaException(id));
        Agregado agregado = lectura.agregado(id);
        return new FichaEspecie(especie.id(), especie.nombreCientifico(), especie.nombreComun(),
                agregado.total(), agregado.ultima(),
                List.copyOf(lectura.regiones(id)), List.copyOf(lectura.recientes(id, HALLAZGOS_RECIENTES)));
    }
}
