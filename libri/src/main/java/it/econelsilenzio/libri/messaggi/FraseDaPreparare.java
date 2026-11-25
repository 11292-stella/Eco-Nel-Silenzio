package it.econelsilenzio.libri.messaggi;

/**
 * Messaggio per RabbitMQ: una frase di cui voce deve preparare l'audio in anticipo.
 * Viaggia in JSON, per esempio:
 * {"libroId":5,"pagina":4,"testo":"Era una notte buia e il vento soffiava sul porto."}
 */
public record FraseDaPreparare(
        Long libroId,      // solo per i log di voce: "preparo libro 5, pagina 4"
        int pagina,
        String testo       // l'unica cosa che serve davvero a voce
) {}