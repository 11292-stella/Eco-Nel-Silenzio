package it.econelsilenzio.libri.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Dove libri manda le frasi da preparare in anticipo (RabbitMQ).
 * Valori in application.properties, sotto "eco.preparazione".
 */
@ConfigurationProperties(prefix = "eco.preparazione")
public record PreparazioneProperties(
        @DefaultValue("eco.voce") String exchange,     // lo smistatore dove si pubblica
        @DefaultValue("prepara") String routingKey,    // l'etichetta dei messaggi
        @DefaultValue("1") int pagineAvanti            // quante pagine dopo quella aperta
) {}
