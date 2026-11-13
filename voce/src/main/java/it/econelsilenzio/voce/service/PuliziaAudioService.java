package it.econelsilenzio.voce.service;

import it.econelsilenzio.voce.config.VoceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Tiene in ordine la cartella degli audio (la cache), cosi' non cresce all'infinito.
 * Parte all'avvio del servizio e poi ogni notte alle 3. Tre regole:
 *   1. avanzi: .wav e mp3 temporanei rimasti a meta' (es. servizio chiuso mentre generava), piu' vecchi di 1 ora
 *   2. eta': mp3 non ascoltati da piu' di "giorni-conservazione" giorni
 *   3. dimensione: se la cartella supera "dimensione-massima-mb", via i meno usati finche' non rientra
 * "Usato" = data di ultima modifica, che SintesiService aggiorna a ogni ascolto (logica LRU).
 * Tocca SOLO i file suoi (mp3 e temporanei): qualsiasi altro file nella cartella resta dov'e'.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PuliziaAudioService {

    // Un file temporaneo piu' vecchio di cosi' non e' "in lavorazione": e' un avanzo
    private static final Duration ETA_AVANZI = Duration.ofHours(1);

    private final VoceProperties props;

    /** Il risultato di una pulizia: per il log e per i test. */
    public record EsitoPulizia(int fileCancellati, long byteLiberati, long byteInCache) {}

    // Un mp3 della cache, con le informazioni che servono per decidere
    private record FileAudio(Path percorso, Instant ultimoUso, long dimensione) {}

    // All'avvio: se il servizio e' rimasto spento per settimane, si pulisce subito
    @EventListener(ApplicationReadyEvent.class)
    public void allAvvio() {
        pulisciSenzaErrori();
    }

    // Ogni notte alle 3 (secondi minuti ore giorno mese giorno-della-settimana)
    @Scheduled(cron = "0 0 3 * * *")
    public void ogniNotte() {
        pulisciSenzaErrori();
    }

    // La pulizia non deve MAI far cadere il servizio: se qualcosa va storto, lo scrive nel log e basta
    private void pulisciSenzaErrori() {
        try {
            pulisci();
        } catch (Exception e) {
            log.warn("Pulizia degli audio non riuscita: {}", e.getMessage());
        }
    }

    /**
     * Pulisce la cartella degli audio. synchronized: due pulizie insieme (avvio + notte) non si pestano i piedi.
     */
    public synchronized EsitoPulizia pulisci() throws IOException {
        Path cartella = Path.of(props.audio().cartella());
        if (!Files.isDirectory(cartella)) {
            return new EsitoPulizia(0, 0, 0);                  // niente cartella = niente da pulire
        }

        Instant adesso = Instant.now();
        Instant limiteAudio = adesso.minus(Duration.ofDays(props.audio().giorniConservazione()));
        Instant limiteAvanzi = adesso.minus(ETA_AVANZI);

        int cancellati = 0;
        long liberati = 0;
        List<FileAudio> rimasti = new ArrayList<>();            // gli mp3 sopravvissuti alle regole 1 e 2

        // --- Regole 1 e 2: avanzi e mp3 troppo vecchi ---
        try (Stream<Path> elenco = Files.list(cartella)) {
            for (Path file : elenco.toList()) {
                if (!Files.isRegularFile(file)) continue;

                String nome = file.getFileName().toString();
                BasicFileAttributes info = Files.readAttributes(file, BasicFileAttributes.class);
                Instant ultimoUso = info.lastModifiedTime().toInstant();
                long dimensione = info.size();

                boolean avanzo = nome.startsWith("piper-") || nome.startsWith("voce-");   // i temporanei di SintesiService
                boolean audio = nome.endsWith(".mp3") && !avanzo;

                boolean daCancellare = (avanzo && ultimoUso.isBefore(limiteAvanzi))
                        || (audio && ultimoUso.isBefore(limiteAudio));

                if (daCancellare) {
                    if (cancella(file)) {
                        cancellati++;
                        liberati += dimensione;
                    }
                } else if (audio) {
                    rimasti.add(new FileAudio(file, ultimoUso, dimensione));
                }
            }
        }

        // --- Regola 3: la cartella e' ancora troppo grande? Via i meno usati ---
        long inCache = rimasti.stream().mapToLong(FileAudio::dimensione).sum();
        long massimo = props.audio().dimensioneMassimaMb() * 1024L * 1024L;

        if (inCache > massimo) {
            rimasti.sort(Comparator.comparing(FileAudio::ultimoUso));   // dal meno usato al piu' usato
            for (FileAudio f : rimasti) {
                if (inCache <= massimo) break;
                if (cancella(f.percorso())) {
                    cancellati++;
                    liberati += f.dimensione();
                    inCache -= f.dimensione();
                }
            }
        }

        log.info("Pulizia audio: {} file cancellati ({} MB liberati), in cache {} MB",
                cancellati, mega(liberati), mega(inCache));
        return new EsitoPulizia(cancellati, liberati, inCache);
    }

    // Cancella un file; se non ci riesce (es. su Windows e' aperto in quel momento) lo lascia e riprova la prossima volta
    private boolean cancella(Path file) {
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            log.debug("Impossibile cancellare {}: {}", file.getFileName(), e.getMessage());
            return false;
        }
    }

    // byte → "12,3"
    private static String mega(long byteCount) {
        return String.format("%.1f", byteCount / (1024.0 * 1024.0));
    }
}