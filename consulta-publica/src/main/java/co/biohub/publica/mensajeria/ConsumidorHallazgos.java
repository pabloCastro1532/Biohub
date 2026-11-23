package co.biohub.publica.mensajeria;

import co.biohub.publica.servicio.InvalidadorCache;
import co.biohub.publica.servicio.ProyeccionHallazgos;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/** Recibe hallazgo.registrado, actualiza la BD de lectura y, cuando hubo cambio, invalida la caché de esa especie. */
@Component
public class ConsumidorHallazgos {

    private final ProyeccionHallazgos proyeccion;
    private final InvalidadorCache cache;

    public ConsumidorHallazgos(ProyeccionHallazgos proyeccion, InvalidadorCache cache) {
        this.proyeccion = proyeccion;
        this.cache = cache;
    }

    @RabbitListener(queues = RabbitConfig.COLA_HALLAZGOS)
    public void alRecibir(Map<String, Object> mensaje) {
        // Si la BD falla, la excepción sale y RabbitMQ reintenta (application.yml). La caché se invalida DESPUÉS de confirmar.
        Optional<Long> especie = proyeccion.aplicar(mensaje);
        especie.ifPresent(cache::invalidar);
    }
}
