package co.biohub.publica.servicio;

import co.biohub.publica.repositorio.LecturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

/**
 * Mantiene la BD de lectura al día a partir de los eventos hallazgo.registrado (contratos/README.md):
 *  - idempotente: un evento repetido no cambia nada;
 *  - solo avanza: una versión más vieja que la guardada se ignora;
 *  - nunca guarda coordenadas (el contrato no las trae).
 * Un mensaje mal formado se descarta (reintentarlo no lo arregla). Un fallo de la BD sí lanza excepción,
 * para que RabbitMQ lo reintente.
 */
@Service
public class ProyeccionHallazgos {

    private static final Logger LOG = LoggerFactory.getLogger(ProyeccionHallazgos.class);

    private final LecturaRepository lectura;

    public ProyeccionHallazgos(LecturaRepository lectura) {
        this.lectura = lectura;
    }

    /** Devuelve la especie afectada (para invalidar su caché) o vacío si el evento no cambió nada. */
    @Transactional
    @SuppressWarnings("unchecked")
    public Optional<Long> aplicar(Map<String, Object> mensaje) {
        String idEvento = mensaje.get("id_evento") instanceof String s && !s.isBlank() ? s : null;
        Long hallazgoId = numero(mensaje.get("hallazgo_id"));
        Long especieId = numero(mensaje.get("especie_id"));
        Long version = numero(mensaje.get("version"));
        String region = mensaje.get("region") instanceof String r && !r.isBlank() ? r : null;
        LocalDate fecha = fecha(mensaje.get("fecha"));

        if (idEvento == null || hallazgoId == null || especieId == null || version == null || version < 1
                || region == null || fecha == null) {
            LOG.warn("Evento hallazgo.registrado descartado: faltan datos obligatorios");
            return Optional.empty();
        }
        Map<String, Object> atributos = mensaje.get("atributos_publicos") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();

        if (!lectura.registrarEvento(idEvento)) {
            LOG.info("Evento {} ya procesado, se ignora", idEvento);
            return Optional.empty();
        }
        lectura.asegurarEspecie(especieId);

        HallazgoPublico nuevo = new HallazgoPublico(hallazgoId, especieId, version.intValue(), region, fecha, atributos);
        Optional<Integer> actual = lectura.versionDe(hallazgoId);
        if (actual.isEmpty()) {
            lectura.insertarHallazgo(nuevo);
        } else if (version > actual.get()) {
            lectura.actualizarHallazgo(nuevo);
        } else {
            LOG.info("Hallazgo {}: la versión {} no es más nueva que la guardada ({}), se ignora",
                    hallazgoId, version, actual.get());
            return Optional.empty();
        }
        LOG.info("Hallazgo {} versión {} aplicado a la BD de lectura", hallazgoId, version);
        return Optional.of(especieId);
    }

    private static Long numero(Object valor) {
        return valor instanceof Number n ? n.longValue() : null;
    }

    private static LocalDate fecha(Object valor) {
        if (!(valor instanceof String texto)) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
