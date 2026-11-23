package co.biohub.cientifico.mensajeria;

import co.biohub.cientifico.servicio.HallazgoRegistradoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publica los eventos del servicio científico en RabbitMQ, solo después de confirmar la transacción.
 * Si RabbitMQ no responde, el hallazgo ya quedó guardado: se registra el fallo y se sigue (no se pierde el dato).
 */
@Component
public class PublicadorEventos {

    private static final Logger LOG = LoggerFactory.getLogger(PublicadorEventos.class);

    private final AmqpTemplate rabbit;

    public PublicadorEventos(AmqpTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarHallazgo(HallazgoRegistradoEvento e) {
        // Mismo contenido que contratos/hallazgo-registrado.schema.json. Sin coordenadas exactas.
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("id_evento", UUID.randomUUID().toString());
        mensaje.put("ocurrido_en", OffsetDateTime.now(ZoneOffset.UTC).toString());
        mensaje.put("hallazgo_id", e.hallazgoId());
        mensaje.put("especie_id", e.especieId());
        mensaje.put("version", e.version());
        mensaje.put("region", e.region());
        mensaje.put("fecha", e.fecha().toString());
        mensaje.put("atributos_publicos", e.atributos());

        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE_EVENTOS, "hallazgo.registrado", mensaje);
            LOG.info("Evento hallazgo.registrado publicado (hallazgo {}, versión {})", e.hallazgoId(), e.version());
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo publicar hallazgo.registrado del hallazgo {}: {}", e.hallazgoId(), ex.getMessage());
        }
    }
}
