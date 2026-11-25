package it.econelsilenzio.voce.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ lato voce (consumer): la coda da cui leggere, l'exchange di libri
 * e il binding che li collega. Spring li crea su RabbitMQ al primo collegamento
 * (se esistono gia' uguali, li lascia come sono).
 */
@Configuration
public class RabbitConfig {

    // La coda "voce.preparazione": durable = sopravvive al riavvio di RabbitMQ
    @Bean
    public Queue codaPreparazione(PreparazioneProperties props) {
        return QueueBuilder.durable(props.coda()).build();
    }

    // Lo stesso exchange di libri: chi arriva per primo lo crea, l'altro lo trova gia' pronto
    @Bean
    public DirectExchange exchangePreparazione(PreparazioneProperties props) {
        return ExchangeBuilder.directExchange(props.exchange())
                .durable(true)
                .build();
    }

    // La regola: dall'exchange "eco.voce", i messaggi "prepara" vanno nella coda "voce.preparazione"
    @Bean
    public Binding bindingPreparazione(Queue codaPreparazione, DirectExchange exchangePreparazione,
                                       PreparazioneProperties props) {
        return BindingBuilder.bind(codaPreparazione)
                .to(exchangePreparazione)
                .with(props.routingKey());
    }

    // I messaggi arrivano in JSON e diventano il record FraseDaPreparare di voce
    @Bean
    public MessageConverter convertitoreJson() {
        return new JacksonJsonMessageConverter();
    }
}