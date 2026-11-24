package co.biohub.publica.mensajeria;

import co.biohub.publica.servicio.InvalidadorCache;
import co.biohub.publica.servicio.ProyeccionEspecies;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Recibe especie.registrada, guarda el nombre en la BD de lectura e invalida la caché de búsquedas. */
@Component
public class ConsumidorEspecies {

    private final ProyeccionEspecies proyeccion;
    private final InvalidadorCache cache;

    public ConsumidorEspecies(ProyeccionEspecies proyeccion, InvalidadorCache cache) {
        this.proyeccion = proyeccion;
        this.cache = cache;
    }

    @RabbitListener(queues = RabbitConfig.COLA_ESPECIES)
    public void alRecibir(Map<String, Object> mensaje) {
        // Si la BD falla, la excepción sale y RabbitMQ reintenta (application.yml). La caché se invalida DESPUÉS de guardar.
        proyeccion.aplicar(mensaje).ifPresent(cache::invalidar);
    }
}
