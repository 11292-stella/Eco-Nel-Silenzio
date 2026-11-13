package it.econelsilenzio.voce.service;

import com.sun.net.httpserver.HttpServer;
import it.econelsilenzio.voce.config.VoceProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test di SintesiService con un SERVER DI PIPER FINTO, avviato dentro il test.
 * Niente Docker e niente Piper vero: decidiamo noi cosa risponde "Piper".
 */
class SintesiServiceTest {

    @TempDir
    Path cartella;                              // la cartella degli mp3, cancellata a fine test

    private HttpServer piperFinto;
    private String urlPiper;

    // Cosa ha ricevuto il server finto
    private final AtomicInteger chiamate = new AtomicInteger();
    private volatile String corpoRicevuto;
    private volatile String tipoRicevuto;

    // Cosa risponde il server finto (ogni test puo' cambiarlo)
    private volatile int statoRisposta = 200;
    private volatile byte[] rispostaPiper;

    @BeforeEach
    void avviaPiperFinto() throws Exception {
        rispostaPiper = wavFinto();

        // Porta 0 = una porta libera qualsiasi, scelta dal sistema
        piperFinto = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        piperFinto.createContext("/synthesize", scambio -> {
            chiamate.incrementAndGet();
            corpoRicevuto = new String(scambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            tipoRicevuto = scambio.getRequestHeaders().getFirst("Content-Type");

            byte[] risposta = rispostaPiper;
            // -1 = risposta senza corpo
            scambio.sendResponseHeaders(statoRisposta, risposta.length == 0 ? -1 : risposta.length);
            if (risposta.length > 0) {
                try (OutputStream out = scambio.getResponseBody()) {
                    out.write(risposta);
                }
            }
            scambio.close();
        });
        piperFinto.start();

        urlPiper = "http://127.0.0.1:" + piperFinto.getAddress().getPort() + "/synthesize";
    }

    @AfterEach
    void fermaPiperFinto() {
        piperFinto.stop(0);
    }

    @Test
    @DisplayName("cache: se l'mp3 della frase esiste gia', Piper non viene chiamato")
    void cache() throws IOException {
        SintesiService service = service(urlPiper);
        String testo = "Era una notte buia.";

        // Mettiamo noi l'mp3 nella cartella, con il nome che il service si aspetta
        Path giaPronto = cartella.resolve(service.nomeFile(testo) + ".mp3");
        Files.writeString(giaPronto, "audio gia' generato");

        Path risultato = service.sintetizza(testo);

        assertThat(risultato).isEqualTo(giaPronto);
        assertThat(chiamate.get()).isZero();
    }

    @Test
    @DisplayName("Piper spento → SintesiException (diventa 503) e nessun file lasciato a meta'")
    void piperSpento() {
        // Porta 1: non c'e' nessuno in ascolto
        SintesiService service = service("http://127.0.0.1:1/synthesize");

        assertThatThrownBy(() -> service.sintetizza("Ciao."))
                .isInstanceOf(SintesiException.class);

        assertThat(cartella).isEmptyDirectory();   // i file temporanei sono stati cancellati
    }

    @Test
    @DisplayName("Piper risponde 500 → SintesiException e nessun file lasciato a meta'")
    void piperInErrore() {
        statoRisposta = 500;
        rispostaPiper = "errore interno".getBytes();

        assertThatThrownBy(() -> service(urlPiper).sintetizza("Ciao."))
                .isInstanceOf(SintesiException.class);

        assertThat(chiamate.get()).isEqualTo(1);
        assertThat(cartella).isEmptyDirectory();
    }

    @Test
    @DisplayName("Piper risponde senza audio → SintesiException")
    void piperAudioVuoto() {
        rispostaPiper = new byte[0];

        assertThatThrownBy(() -> service(urlPiper).sintetizza("Ciao."))
                .isInstanceOf(SintesiException.class);

        assertThat(cartella).isEmptyDirectory();
    }

    @Test
    @EnabledIf("ffmpegInstallato")   // senza ffmpeg (es. su GitHub Actions) il test viene SALTATO, non fallisce
    @DisplayName("frase nuova: a Piper arriva il JSON giusto (accenti e «» intatti), nasce l'mp3, la seconda volta usa la cache")
    void sintesiCompleta() throws IOException {
        SintesiService service = service(urlPiper);
        String testo = "«Più forte», disse il sig. Rossi.";

        Path mp3 = service.sintetizza(testo);

        // Il JSON mandato a Piper
        assertThat(tipoRicevuto).startsWith("application/json");
        assertThat(corpoRicevuto)
                .contains("\"text\":\"«Più forte», disse il sig. Rossi.\"")
                .contains("\"length_scale\":1.1")
                .contains("\"noise_scale\":0.5");

        // L'mp3 creato da ffmpeg
        assertThat(mp3).exists();
        assertThat(Files.size(mp3)).isPositive();
        assertThat(mp3.getFileName().toString()).endsWith(".mp3");

        // Seconda volta: stesso file, Piper non viene richiamato
        assertThat(service.sintetizza(testo)).isEqualTo(mp3);
        assertThat(chiamate.get()).isEqualTo(1);

        // Nella cartella resta solo l'mp3: niente .wav o file temporanei
        try (var file = Files.list(cartella)) {
            assertThat(file).containsExactly(mp3);
        }
    }

    // ===== Metodi di supporto =====

    // Il service con le stesse impostazioni di application.properties, ma con il Piper che diciamo noi
    private SintesiService service(String url) {
        VoceProperties props = new VoceProperties(
                new VoceProperties.Piper(url, 1.1, 0.5),
                new VoceProperties.Ffmpeg("ffmpeg", 0.92, 16000),
                new VoceProperties.Audio(cartella.toString(), 7, 500)   // NUOVO: giorni e MB della pulizia
        );
        return new SintesiService(props);
    }

    // Un .wav vero: 0,1 secondi di silenzio, mono, 16000 Hz (come la voce di Riccardo)
    private static byte[] wavFinto() throws IOException {
        AudioFormat formato = new AudioFormat(16000, 16, 1, true, false);
        byte[] silenzio = new byte[16000 * 2 / 10];            // 2 byte per campione, 1/10 di secondo
        AudioInputStream audio = new AudioInputStream(
                new ByteArrayInputStream(silenzio), formato, silenzio.length / 2);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AudioSystem.write(audio, AudioFileFormat.Type.WAVE, out);
        return out.toByteArray();
    }

    // Usato da @EnabledIf: true se il comando "ffmpeg" esiste su questo computer
    static boolean ffmpegInstallato() {
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}