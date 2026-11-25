package it.econelsilenzio.voce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Da dove voce legge le frasi da preparare in anticipo (RabbitMQ).
 * Valori in application.properties, sotto "eco.preparazione".
 * Exchange e routing key devono essere gli stessi di libri.
 */
@ConfigurationProperties(prefix = "eco.preparazione")
public record PreparazioneProperties(
        @DefaultValue("voce.preparazione") String coda,   // la cassetta da cui voce legge
        @DefaultValue("eco.voce") String exchange,        // lo smistatore di libri
        @DefaultValue("prepara") String routingKey        // l'etichetta che porta qui i messaggi
) {}
