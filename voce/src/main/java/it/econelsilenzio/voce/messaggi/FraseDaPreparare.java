package it.econelsilenzio.voce.messaggi;

/**
 * Messaggio da RabbitMQ: una frase di cui preparare l'audio in anticipo.
 * Lo manda libri. Contratto in JSON, uguale a it.econelsilenzio.libri.messaggi.FraseDaPreparare:
 * {"libroId":5,"pagina":4,"testo":"Era una notte buia e il vento soffiava sul porto."}
 * Se cambi i campi qui, cambiali anche in libri.
 */
public record FraseDaPreparare(
        Long libroId,      // solo per i log: "preparo libro 5, pagina 4"
        int pagina,
        String testo       // quello che Piper deve leggere
) {}