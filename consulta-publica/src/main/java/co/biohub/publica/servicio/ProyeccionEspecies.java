package co.biohub.publica.servicio;

import co.biohub.publica.repositorio.LecturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Mantiene el catálogo de especies de la BD de lectura al día a partir de especie.registrada (contratos/README.md).
 * Es idempotente. Un mensaje mal formado se descarta (reintentarlo no lo arregla); un fallo de la BD lanza excepción
 * para que RabbitMQ lo reintente.
 */
@Service
public class ProyeccionEspecies {

    private static final Logger LOG = LoggerFactory.getLogger(ProyeccionEspecies.class);

    private final LecturaRepository lectura;

    public ProyeccionEspecies(LecturaRepository lectura) {
        this.lectura = lectura;
    }

    /** Devuelve la especie afectada (para invalidar su caché) o vacío si el mensaje no servía. */
    @Transactional
    public Optional<Long> aplicar(Map<String, Object> mensaje) {
        Long especieId = mensaje.get("especie_id") instanceof Number n ? n.longValue() : null;
        String cientifico = mensaje.get("nombre_cientifico") instanceof String s && !s.isBlank() ? s.trim() : null;
        String comun = mensaje.get("nombre_comun") instanceof String c && !c.isBlank() ? c.trim() : null;

        if (especieId == null || cientifico == null) {
            LOG.warn("Evento especie.registrada descartado: faltan datos obligatorios");
            return Optional.empty();
        }
        lectura.guardarEspecie(especieId, cientifico, comun);
        LOG.info("Especie {} ({}) guardada en la BD de lectura", especieId, cientifico);
        return Optional.of(especieId);
    }
}
