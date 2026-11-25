package it.econelsilenzio.libri.service;

import it.econelsilenzio.libri.config.PreparazioneProperties;
import it.econelsilenzio.libri.messaggi.FraseDaPreparare;
import it.econelsilenzio.libri.model.Frase;
import it.econelsilenzio.libri.model.Pagina;
import it.econelsilenzio.libri.repository.PaginaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Quando il lettore apre una pagina, manda in coda le frasi delle pagine successive:
 * voce ne prepara l'audio in background, cosi' quando il lettore ci arriva e' gia' in cache.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreparazioneAudioService {

    private final PaginaRepository paginaRepository;
    private final RabbitTemplate rabbitTemplate;      // creato da Spring Boot, usa il convertitore JSON
    private final PreparazioneProperties props;

    /**
     * Manda in coda le frasi delle pagine dopo "numero".
     * Gira in un altro thread (@Async): la GET della pagina risponde subito, senza aspettare.
     * Non lancia mai eccezioni: la preparazione e' un "di piu'", la lettura funziona anche senza.
     */
    @Async
    @Transactional(readOnly = true)   // frasi e' LAZY: si leggono solo dentro la transazione
    public void preparaDopo(Long libroId, int numero) {
        int inviate = 0;
        try {
            for (int n = numero + 1; n <= numero + props.pagineAvanti(); n++) {
                Optional<Pagina> pagina = paginaRepository.findByLibroIdAndNumero(libroId, n);
                if (pagina.isEmpty()) {
                    break;                                    // fine del libro: niente da preparare
                }
                // gia' in ordine di lettura grazie a @OrderBy("numero ASC") in Pagina
                for (Frase frase : pagina.get().getFrasi()) {
                    rabbitTemplate.convertAndSend(
                            props.exchange(),                 // "eco.voce"
                            props.routingKey(),               // "prepara"
                            new FraseDaPreparare(libroId, n, frase.getTesto()));
                    inviate++;
                }
            }
            log.info("Libro {}, aperta pagina {}: {} frasi mandate in coda per voce", libroId, numero, inviate);
        } catch (AmqpException e) {
            // RabbitMQ spento o irraggiungibile: si legge lo stesso, l'audio si genera al momento
            log.warn("Preparazione in anticipo non riuscita (libro {}, pagina {}): {}",
                    libroId, numero, e.getMessage());
        }
    }
}