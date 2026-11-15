package it.econelsilenzio.immagini.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Le impostazioni del servizio immagini, lette da application.properties (prefisso "immagini").
 *
 * immagini.catalogo.cartella=catalogo   →  catalogo().cartella()
 */
@ConfigurationProperties(prefix = "immagini")
public record ImmaginiProperties(Catalogo catalogo) {

    /** Il catalogo: una cartella per tema, con dentro le immagini. */
    public record Catalogo(
            @DefaultValue("catalogo") String cartella   // se la riga manca, usa "catalogo"
    ) {}
}
