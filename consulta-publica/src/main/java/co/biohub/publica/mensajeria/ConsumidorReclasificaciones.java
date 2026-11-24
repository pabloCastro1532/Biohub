package co.biohub.publica.mensajeria;

import co.biohub.publica.servicio.InvalidadorCache;
import co.biohub.publica.servicio.ProyeccionHallazgos;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * HU-09: recibe hallazgo.reclasificado, pasa el hallazgo a la especie nueva en la BD de lectura y, solo después de
 * guardar, invalida la caché de las dos especies (la que lo pierde y la que lo gana) y de las búsquedas.
 */
@Component
public class ConsumidorReclasificaciones {

    private final ProyeccionHallazgos proyeccion;
    private final InvalidadorCache cache;

    public ConsumidorReclasificaciones(ProyeccionHallazgos proyeccion, InvalidadorCache cache) {
        this.proyeccion = proyeccion;
        this.cache = cache;
    }

    @RabbitListener(queues = RabbitConfig.COLA_RECLASIFICACIONES)
    public void alRecibir(Map<String, Object> mensaje) {
        // Si la BD falla, la excepción sale y RabbitMQ reintenta (application.yml). La caché se invalida DESPUÉS de guardar.
        proyeccion.reclasificar(mensaje).forEach(cache::invalidar);
    }
}
