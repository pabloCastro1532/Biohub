package co.biohub.cientifico.mensajeria;

import co.biohub.cientifico.servicio.MonitorSeguridad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

/** Recibe acceso.coordenadas.registrado y se lo pasa al monitor de seguridad (HU-26). */
@Component
public class ConsumidorAccesos {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorAccesos.class);

    private final MonitorSeguridad monitor;

    public ConsumidorAccesos(MonitorSeguridad monitor) {
        this.monitor = monitor;
    }

    @RabbitListener(queues = ColasSeguridad.COLA_ACCESOS)
    public void alRecibir(Map<String, Object> mensaje) {
        try {
            Long usuarioId = ((Number) mensaje.get("usuario_id")).longValue();
            // Se usa la hora del propio evento (la misma que quedó en registro_acceso), no la del reloj de este servicio.
            LocalDateTime ocurridoEn = OffsetDateTime.parse((String) mensaje.get("ocurrido_en")).toLocalDateTime();
            monitor.evaluar(usuarioId, ocurridoEn);
        } catch (RuntimeException e) {
            // No se relanza: un mensaje roto devuelto a la cola se reintentaría para siempre.
            // El acceso ya está guardado en registro_acceso, así que no se pierde la constancia.
            LOG.warn("Evento de acceso descartado ({}): {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
