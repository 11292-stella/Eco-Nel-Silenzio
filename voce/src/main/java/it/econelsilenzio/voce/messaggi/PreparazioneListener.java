package it.econelsilenzio.voce.messaggi;

import it.econelsilenzio.voce.service.SintesiException;
import it.econelsilenzio.voce.service.SintesiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Legge dalla coda le frasi mandate da libri e ne prepara l'audio in anticipo.
 * L'mp3 finisce nella stessa cache di GET /api/voce/sintesi: quando il lettore
 * arriva a quella frase, l'audio e' gia' pronto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreparazioneListener {

    // Lo stesso limite del controller (@Size(max = 1000)): oltre, il frontend non la chiederebbe mai
    static final int MAX_CARATTERI = 1000;

    private final SintesiService sintesiService;

    /**
     * Una frase alla volta (concurrency e prefetch = 1 in application.properties).
     * Se il metodo finisce normalmente → ack, il messaggio sparisce dalla coda.
     */
    @RabbitListener(queues = "${eco.preparazione.coda}")
    public void prepara(FraseDaPreparare frase) {
        // trim come nel controller: stesso testo = stesso nome dell'mp3 in cache
        String testo = frase.testo() == null ? "" : frase.testo().trim();

        if (testo.isEmpty() || testo.length() > MAX_CARATTERI) {
            log.warn("Frase scartata (libro {}, pagina {}): vuota o oltre {} caratteri",
                    frase.libroId(), frase.pagina(), MAX_CARATTERI);
            return;                                   // ack: un messaggio sbagliato non va riprovato
        }

        try {
            log.debug("Preparo libro {}, pagina {}: {}", frase.libroId(), frase.pagina(), testo);
            sintesiService.sintetizza(testo);         // gia' in cache → non chiama Piper
        } catch (SintesiException e) {
            // Piper spento o ffmpeg in errore: la frase si generera' al momento della lettura
            log.warn("Preparazione non riuscita (libro {}, pagina {}): {}",
                    frase.libroId(), frase.pagina(), e.getMessage());
        }
    }
}