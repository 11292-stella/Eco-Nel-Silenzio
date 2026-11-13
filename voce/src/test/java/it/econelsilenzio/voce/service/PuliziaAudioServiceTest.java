package it.econelsilenzio.voce.service;

import it.econelsilenzio.voce.config.VoceProperties;
import it.econelsilenzio.voce.service.PuliziaAudioService.EsitoPulizia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test della pulizia degli audio: cartella temporanea e file con date "finte",
 * cosi' si prova "10 giorni fa" senza aspettare 10 giorni.
 */
class PuliziaAudioServiceTest {

    private static final int KB = 1024;

    @TempDir
    Path cartella;

    @Test
    @DisplayName("regola dell'eta': via gli mp3 non usati da piu' di 7 giorni, restano quelli recenti")
    void eta() throws IOException {
        Path vecchio = creaFile("a1b2.mp3", 20 * KB, Duration.ofDays(10));
        Path recente = creaFile("c3d4.mp3", 20 * KB, Duration.ofDays(2));

        EsitoPulizia esito = pulizia(7, 500).pulisci();

        assertThat(vecchio).doesNotExist();
        assertThat(recente).exists();
        assertThat(esito.fileCancellati()).isEqualTo(1);
        assertThat(esito.byteLiberati()).isEqualTo(20 * KB);
        assertThat(esito.byteInCache()).isEqualTo(20 * KB);
    }

    @Test
    @DisplayName("avanzi: via i temporanei piu' vecchi di 1 ora, restano quelli 'in lavorazione'")
    void avanzi() throws IOException {
        Path wavVecchio = creaFile("piper-123.wav", 50 * KB, Duration.ofHours(2));
        Path mp3Vecchio = creaFile("voce-456.mp3", 10 * KB, Duration.ofHours(2));
        Path inLavorazione = creaFile("piper-789.wav", 50 * KB, Duration.ofMinutes(5));

        pulizia(7, 500).pulisci();

        assertThat(wavVecchio).doesNotExist();
        assertThat(mp3Vecchio).doesNotExist();
        assertThat(inLavorazione).exists();     // Piper/ffmpeg potrebbero starlo usando proprio ora
    }

    @Test
    @DisplayName("regola della dimensione: oltre il limite, via i MENO usati finche' non rientra")
    void dimensione() throws IOException {
        // 3 file da 600 KB = 1,8 MB, limite 1 MB → devono sparire i due meno usati
        Path menoUsato = creaFile("aaaa.mp3", 600 * KB, Duration.ofDays(3));
        Path medio = creaFile("bbbb.mp3", 600 * KB, Duration.ofDays(2));
        Path piuUsato = creaFile("cccc.mp3", 600 * KB, Duration.ofHours(1));

        EsitoPulizia esito = pulizia(7, 1).pulisci();

        assertThat(menoUsato).doesNotExist();
        assertThat(medio).doesNotExist();
        assertThat(piuUsato).exists();
        assertThat(esito.fileCancellati()).isEqualTo(2);
        assertThat(esito.byteInCache()).isEqualTo(600 * KB).isLessThanOrEqualTo(1024 * KB);
    }

    @Test
    @DisplayName("la pulizia tocca solo i suoi file: un altro file nella cartella resta dov'e'")
    void soloIFileSuoi() throws IOException {
        Path nota = creaFile("leggimi.txt", 1 * KB, Duration.ofDays(100));

        EsitoPulizia esito = pulizia(7, 500).pulisci();

        assertThat(nota).exists();
        assertThat(esito.fileCancellati()).isZero();
    }

    @Test
    @DisplayName("cartella degli audio che non esiste ancora → nessun errore, niente da pulire")
    void cartellaInesistente() throws IOException {
        VoceProperties props = props(cartella.resolve("non-ancora-creata"), 7, 500);

        EsitoPulizia esito = new PuliziaAudioService(props).pulisci();

        assertThat(esito).isEqualTo(new EsitoPulizia(0, 0, 0));
    }

    @Test
    @DisplayName("LRU: un audio vecchio ma appena riascoltato (cache di SintesiService) NON viene cancellato")
    void appenaRiascoltato() throws IOException {
        VoceProperties props = props(cartella, 7, 500);
        // Piper sulla porta 1 (spento): se il test lo chiamasse fallirebbe, quindi prova anche che si usa la cache
        SintesiService sintesi = new SintesiService(props);
        String testo = "Era una notte buia.";

        Path mp3 = creaFile(sintesi.nomeFile(testo) + ".mp3", 20 * KB, Duration.ofDays(10));

        sintesi.sintetizza(testo);                      // la frase esce dalla cache → la data diventa "adesso"
        new PuliziaAudioService(props).pulisci();

        assertThat(mp3).exists();
    }

    // ===== Metodi di supporto =====

    // Un file di "dimensione" byte, con la data di ultimo uso spostata indietro di "fa"
    private Path creaFile(String nome, int dimensione, Duration fa) throws IOException {
        Path file = cartella.resolve(nome);
        Files.write(file, new byte[dimensione]);
        Files.setLastModifiedTime(file, FileTime.from(Instant.now().minus(fa)));
        return file;
    }

    private PuliziaAudioService pulizia(int giorni, int megaMassimi) {
        return new PuliziaAudioService(props(cartella, giorni, megaMassimi));
    }

    private static VoceProperties props(Path cartellaAudio, int giorni, int megaMassimi) {
        return new VoceProperties(
                new VoceProperties.Piper("http://127.0.0.1:1/synthesize", 1.1, 0.5),
                new VoceProperties.Ffmpeg("ffmpeg", 0.92, 16000),
                new VoceProperties.Audio(cartellaAudio.toString(), giorni, megaMassimi)
        );
    }
}