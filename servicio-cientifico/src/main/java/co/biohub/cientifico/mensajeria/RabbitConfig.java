package co.biohub.cientifico.mensajeria;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exchange de eventos de BioHub (ver contratos/README.md): un topic durable. Los mensajes viajan como JSON. */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTOS = "biohub.eventos";

    @Bean
    public TopicExchange exchangeEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public MessageConverter convertidorJson() {
        return new Jackson2JsonMessageConverter();
    }
}
