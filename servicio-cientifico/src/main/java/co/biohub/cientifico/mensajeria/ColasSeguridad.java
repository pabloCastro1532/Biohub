package co.biohub.cientifico.mensajeria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cola durable del monitor de seguridad (contratos/README.md): recibe acceso.coordenadas.registrado. */
@Configuration
public class ColasSeguridad {

    public static final String COLA_ACCESOS = "seguridad.accesos";

    @Bean
    public Queue colaAccesos() {
        return new Queue(COLA_ACCESOS, true);
    }

    @Bean
    public Binding enlaceAccesos(TopicExchange exchangeEventos) {
        return BindingBuilder.bind(colaAccesos()).to(exchangeEventos).with("acceso.coordenadas.registrado");
    }
}
