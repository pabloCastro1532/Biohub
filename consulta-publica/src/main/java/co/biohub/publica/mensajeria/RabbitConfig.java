package co.biohub.publica.mensajeria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de contratos/README.md: exchange topic durable "biohub.eventos" y la cola durable de este servicio.
 * Las dos instancias de consulta-publica comparten la cola: cada mensaje lo recibe una sola.
 * La cola de especies recibe especie.registrada, para conocer el nombre de las especies que se agregan.
 * La cola de reclasificaciones recibe hallazgo.reclasificado (HU-09): mueve el hallazgo de especie y limpia la caché de las dos.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTOS = "biohub.eventos";
    public static final String COLA_HALLAZGOS = "consulta-publica.hallazgos";
    public static final String COLA_ESPECIES = "consulta-publica.especies";
    public static final String COLA_RECLASIFICACIONES = "consulta-publica.reclasificaciones";

    @Bean
    public TopicExchange exchangeEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public Queue colaHallazgos() {
        return new Queue(COLA_HALLAZGOS, true);
    }

    @Bean
    public Binding enlaceHallazgoRegistrado(TopicExchange exchangeEventos) {
        return BindingBuilder.bind(colaHallazgos()).to(exchangeEventos).with("hallazgo.registrado");
    }

    @Bean
    public Queue colaEspecies() {
        return new Queue(COLA_ESPECIES, true);
    }

    @Bean
    public Binding enlaceEspecieRegistrada(TopicExchange exchangeEventos) {
        return BindingBuilder.bind(colaEspecies()).to(exchangeEventos).with("especie.registrada");
    }

    @Bean
    public Queue colaReclasificaciones() {
        return new Queue(COLA_RECLASIFICACIONES, true);
    }

    @Bean
    public Binding enlaceHallazgoReclasificado(TopicExchange exchangeEventos) {
        return BindingBuilder.bind(colaReclasificaciones()).to(exchangeEventos).with("hallazgo.reclasificado");
    }

    @Bean
    public MessageConverter convertidorJson() {
        return new Jackson2JsonMessageConverter();
    }
}
