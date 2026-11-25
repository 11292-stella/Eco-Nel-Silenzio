package it.econelsilenzio.libri.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ lato libri (producer): l'exchange dove pubblicare e il formato dei messaggi.
 * La coda e il binding li dichiara voce, che e' chi legge.
 */
@Configuration
public class RabbitConfig {

    // L'exchange "eco.voce": di tipo DIRECT = smista confrontando la routing key esatta ("prepara")
    @Bean
    public DirectExchange exchangePreparazione(PreparazioneProperties props) {
        return ExchangeBuilder.directExchange(props.exchange())
                .durable(true)          // sopravvive al riavvio di RabbitMQ
                .build();
    }

    // I messaggi viaggiano in JSON: {"testo":"Era una notte buia..."}
    // Spring lo usa da solo nel RabbitTemplate (Jackson 3, come il resto di Spring Boot 4)
    @Bean
    public MessageConverter convertitoreJson() {
        return new JacksonJsonMessageConverter();
    }
}