package co.biohub.cientifico.mensajeria;

import co.biohub.cientifico.servicio.EspecieRegistradaEvento;
import co.biohub.cientifico.servicio.HallazgoReclasificadoEvento;
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarEspecie(EspecieRegistradaEvento e) {
        // Mismo contenido que contratos/especie-registrada.schema.json.
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("id_evento", UUID.randomUUID().toString());
        mensaje.put("ocurrido_en", OffsetDateTime.now(ZoneOffset.UTC).toString());
        mensaje.put("especie_id", e.especieId());
        mensaje.put("nombre_cientifico", e.nombreCientifico());
        mensaje.put("nombre_comun", e.nombreComun());

        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE_EVENTOS, "especie.registrada", mensaje);
            LOG.info("Evento especie.registrada publicado (especie {})", e.especieId());
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo publicar especie.registrada de la especie {}: {}", e.especieId(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alReclasificarHallazgo(HallazgoReclasificadoEvento e) {
        // Mismo contenido que contratos/hallazgo-reclasificado.schema.json.
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("id_evento", UUID.randomUUID().toString());
        mensaje.put("ocurrido_en", OffsetDateTime.now(ZoneOffset.UTC).toString());
        mensaje.put("hallazgo_id", e.hallazgoId());
        mensaje.put("especie_anterior", e.especieAnterior());
        mensaje.put("especie_nueva", e.especieNueva());
        mensaje.put("autor", e.autor());
        mensaje.put("motivo", e.motivo());

        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE_EVENTOS, "hallazgo.reclasificado", mensaje);
            LOG.info("Evento hallazgo.reclasificado publicado (hallazgo {}: especie {} -> {})",
                    e.hallazgoId(), e.especieAnterior(), e.especieNueva());
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo publicar hallazgo.reclasificado del hallazgo {}: {}", e.hallazgoId(), ex.getMessage());
        }
    }
}
