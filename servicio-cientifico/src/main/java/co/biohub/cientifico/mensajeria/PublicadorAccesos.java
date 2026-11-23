package co.biohub.cientifico.mensajeria;

import co.biohub.cientifico.servicio.AccesoCoordenadasEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publica acceso.coordenadas.registrado (contratos/acceso-coordenadas-registrado.schema.json) al confirmar el registro.
 * Alimenta al monitor de seguridad (HU-26). Si RabbitMQ no responde, el acceso ya quedó guardado en la tabla.
 */
@Component
public class PublicadorAccesos {

    private static final Logger LOG = LoggerFactory.getLogger(PublicadorAccesos.class);

    private final AmqpTemplate rabbit;

    public PublicadorAccesos(AmqpTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarAcceso(AccesoCoordenadasEvento e) {
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("id_evento", UUID.randomUUID().toString());
        mensaje.put("ocurrido_en", e.ocurridoEn().atOffset(ZoneOffset.UTC).toString());
        mensaje.put("usuario_id", e.usuarioId());
        mensaje.put("hallazgo_id", e.hallazgoId());
        mensaje.put("resultado", e.resultado().name());

        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE_EVENTOS, "acceso.coordenadas.registrado", mensaje);
            LOG.info("Evento acceso.coordenadas.registrado publicado (usuario {}, hallazgo {}, {})",
                    e.usuarioId(), e.hallazgoId(), e.resultado());
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo publicar acceso.coordenadas.registrado: {}", ex.getMessage());
        }
    }
}
