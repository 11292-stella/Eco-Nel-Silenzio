package it.econelsilenzio.voce.service;

import it.econelsilenzio.voce.config.VoceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Trasforma una frase in un file mp3 con la voce di Riccardo.
 *   1. Piper  → testo in .wav  (chiamata HTTP al server di Piper, sempre acceso)
 *   2. ffmpeg → abbassa la tonalita' e converte in .mp3 (programma esterno)
 * Gli mp3 restano salvati: la stessa frase non viene generata due volte (cache su disco).
 * La data di ogni mp3 = l'ultima volta che e' stato usato: la pulizia cancella quelli non ascoltati da tempo.
 */
@Slf4j
@Service
public class SintesiService {

    // Oltre questo tempo Piper o ffmpeg si considerano bloccati
    private static final long TIMEOUT_SECONDI = 60;

    private final VoceProperties props;
    private final RestClient piperClient;

    public SintesiService(VoceProperties props) {
        this.props = props;

        // Timeout: se il server di Piper e' spento lo sappiamo in 2 secondi, non restiamo appesi
        SimpleClientHttpRequestFactory fabbrica = new SimpleClientHttpRequestFactory();
        fabbrica.setConnectTimeout(Duration.ofSeconds(2));
        fabbrica.setReadTimeout(Duration.ofSeconds(TIMEOUT_SECONDI));

        this.piperClient = RestClient.builder()
                .requestFactory(fabbrica)
                .build();
    }

    /**
     * Restituisce il percorso dell'mp3 della frase: lo genera solo se non esiste gia'.
     */
    public Path sintetizza(String testo) {
        try {
            Path cartella = Path.of(props.audio().cartella());
            Files.createDirectories(cartella);

            Path mp3 = cartella.resolve(nomeFile(testo) + ".mp3");
            if (Files.exists(mp3)) {
                log.debug("Audio gia' pronto: {}", mp3.getFileName());
                segnaComeUsato(mp3);                          // NUOVO: "appena ascoltato", la pulizia lo lascia stare
                return mp3;                                   // cache: niente Piper
            }

            // File temporanei con nome unico: due richieste insieme non si pestano i piedi
            Path wav = Files.createTempFile(cartella, "piper-", ".wav");
            Path mp3Temp = Files.createTempFile(cartella, "voce-", ".mp3");
            try {
                long inizio = System.currentTimeMillis();
                generaWav(testo, wav);
                long dopoPiper = System.currentTimeMillis();
                convertiInMp3(wav, mp3Temp);
                long fine = System.currentTimeMillis();

                // Solo alla fine diventa il file "vero": nessuno legge mai un mp3 a meta'
                Files.move(mp3Temp, mp3, StandardCopyOption.REPLACE_EXISTING);

                // Tempi separati: cosi' sappiamo quanto pesa Piper e quanto ffmpeg
                log.info("Audio generato in {} ms (Piper {} ms, ffmpeg {} ms, {} caratteri): {}",
                        fine - inizio, dopoPiper - inizio, fine - dopoPiper,
                        testo.length(), mp3.getFileName());
                return mp3;
            } finally {
                Files.deleteIfExists(wav);
                Files.deleteIfExists(mp3Temp);
            }
        } catch (IOException e) {
            throw new SintesiException("Impossibile generare l'audio", e);
        }
    }

    // NUOVO: come il comando "touch": la data del file diventa adesso.
    // Se non ci riesce (file bloccato, permessi...) non e' grave: l'audio si ascolta lo stesso,
    // al massimo verra' cancellato un po' prima e poi rigenerato
    private void segnaComeUsato(Path mp3) {
        try {
            Files.setLastModifiedTime(mp3, FileTime.from(Instant.now()));
        } catch (IOException e) {
            log.debug("Impossibile aggiornare la data di {}: {}", mp3.getFileName(), e.getMessage());
        }
    }

    // --- 1. Piper: testo → wav, con una chiamata al suo server HTTP ---
    private void generaWav(String testo, Path wav) throws IOException {
        VoceProperties.Piper piper = props.piper();

        // Lo stesso JSON che abbiamo provato da PowerShell
        Map<String, Object> richiesta = Map.of(
                "text", testo,
                "length_scale", piper.lengthScale(),
                "noise_scale", piper.noiseScale()
        );

        byte[] audio;
        try {
            audio = piperClient.post()
                    .uri(piper.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(richiesta)                      // la Map diventa JSON (UTF-8: accenti e «» ok)
                    .retrieve()
                    .body(byte[].class);                  // la risposta e' il file .wav
        } catch (RestClientException e) {
            // Server spento, errore di Piper, timeout... → 503, il frontend passa alla voce del browser
            log.error("Il server di Piper non risponde su {}: {}", piper.url(), e.getMessage());
            throw new SintesiException("Il server di Piper non e' disponibile", e);
        }

        if (audio == null || audio.length == 0) {
            throw new SintesiException("Piper ha restituito un audio vuoto");
        }
        Files.write(wav, audio);
    }

    // --- 2. ffmpeg: wav → mp3, con la voce piu' bassa ---
    private void convertiInMp3(Path wav, Path mp3) throws IOException {
        VoceProperties.Ffmpeg ffmpeg = props.ffmpeg();
        // es. 16000 * 0.92 = 14720: l'audio "suona" piu' lento, quindi piu' basso
        long frequenzaAbbassata = Math.round(ffmpeg.frequenza() * ffmpeg.tonalita());
        String filtro = "asetrate=" + frequenzaAbbassata + ",aresample=" + ffmpeg.frequenza();

        List<String> comando = List.of(
                ffmpeg.comando(), "-y", "-loglevel", "error",
                "-i", wav.toString(),
                "-af", filtro,
                "-codec:a", "libmp3lame", "-q:a", "4",   // mp3 di buona qualita', leggero
                mp3.toString()
        );
        aspetta(new ProcessBuilder(comando).redirectErrorStream(true).start(), "ffmpeg");
    }

    // Aspetta che il programma finisca; se va male, mette il suo messaggio nel log
    private void aspetta(Process processo, String nome) throws IOException {
        String output;
        try (InputStream out = processo.getInputStream()) {
            output = new String(out.readAllBytes(), StandardCharsets.UTF_8);   // leggere evita che si blocchi
        }
        try {
            if (!processo.waitFor(TIMEOUT_SECONDI, TimeUnit.SECONDS)) {
                processo.destroyForcibly();
                throw new SintesiException(nome + " non ha risposto in " + TIMEOUT_SECONDI + " secondi");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SintesiException(nome + " interrotto", e);
        }
        if (processo.exitValue() != 0) {
            log.error("{} e' terminato con codice {}:\n{}", nome, processo.exitValue(), output);
            throw new SintesiException(nome + " non e' riuscito a generare l'audio");
        }
    }

    // Il nome del file: un'impronta (SHA-256) del testo e delle impostazioni della voce.
    // Stessa frase + stessa voce = stesso nome. Se cambi tonalita', i file si rigenerano.
    // Non e' private: il test della cache lo usa per sapere come si chiamera' l'mp3
    String nomeFile(String testo) {
        String chiave = testo + "|" + props.piper() + "|" + props.ffmpeg();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(chiave.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);   // 32 caratteri bastano
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);              // SHA-256 c'e' sempre in Java
        }
    }
}