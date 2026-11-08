package it.econelsilenzio.voce.service;

import it.econelsilenzio.voce.config.VoceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Trasforma una frase in un file mp3 con la voce di Riccardo.
 * Lancia due programmi esterni, come dal terminale:
 *   1. Piper  → testo in .wav
 *   2. ffmpeg → abbassa la tonalita' e converte in .mp3
 * Gli mp3 restano salvati: la stessa frase non viene generata due volte (cache su disco).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SintesiService {

    // Oltre questo tempo un programma esterno si considera bloccato
    private static final long TIMEOUT_SECONDI = 60;

    private final VoceProperties props;

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
                return mp3;                                   // cache: niente Piper
            }

            // File temporanei con nome unico: due richieste insieme non si pestano i piedi
            Path wav = Files.createTempFile(cartella, "piper-", ".wav");
            Path mp3Temp = Files.createTempFile(cartella, "voce-", ".mp3");
            try {
                long inizio = System.currentTimeMillis();
                generaWav(testo, wav);
                convertiInMp3(wav, mp3Temp);
                // Solo alla fine diventa il file "vero": nessuno legge mai un mp3 a meta'
                Files.move(mp3Temp, mp3, StandardCopyOption.REPLACE_EXISTING);
                log.info("Audio generato in {} ms: {}", System.currentTimeMillis() - inizio, mp3.getFileName());
                return mp3;
            } finally {
                Files.deleteIfExists(wav);
                Files.deleteIfExists(mp3Temp);
            }
        } catch (IOException e) {
            throw new SintesiException("Impossibile generare l'audio", e);
        }
    }

    // --- 1. Piper: testo → wav ---
    private void generaWav(String testo, Path wav) throws IOException {
        VoceProperties.Piper piper = props.piper();
        List<String> comando = List.of(
                piper.comando(), "-m", "piper",
                "-m", piper.modello(),
                "--length-scale", numero(piper.lengthScale()),
                "--noise-scale", numero(piper.noiseScale()),
                "-f", wav.toString()
        );

        ProcessBuilder pb = new ProcessBuilder(comando).redirectErrorStream(true);
        // Python su Windows legge l'input in cp1252: lo obblighiamo a usare UTF-8 (accenti, «»)
        pb.environment().put("PYTHONUTF8", "1");
        pb.environment().put("PYTHONIOENCODING", "utf-8");

        Process processo = pb.start();
        // Il testo arriva a Piper dallo "standard input", come con: echo "testo" | python -m piper ...
        try (OutputStream in = processo.getOutputStream()) {
            in.write(testo.getBytes(StandardCharsets.UTF_8));
        }
        aspetta(processo, "Piper");
    }

    // --- 2. ffmpeg: wav → mp3, con la voce piu' bassa ---
    private void convertiInMp3(Path wav, Path mp3) throws IOException {
        VoceProperties.Ffmpeg ffmpeg = props.ffmpeg();
        // es. 16000 * 0.88 = 14080: l'audio "suona" piu' lento, quindi piu' basso
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
    private String nomeFile(String testo) {
        String chiave = testo + "|" + props.piper() + "|" + props.ffmpeg();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(chiave.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);   // 32 caratteri bastano
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);              // SHA-256 c'e' sempre in Java
        }
    }

    // 1.1 → "1.1" sempre col punto (con il Locale italiano diventerebbe "1,1" e Piper non lo capirebbe)
    private String numero(double valore) {
        return String.format(Locale.ROOT, "%s", valore);
    }
}