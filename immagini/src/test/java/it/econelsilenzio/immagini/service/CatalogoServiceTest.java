package it.econelsilenzio.immagini.service;

import it.econelsilenzio.immagini.config.ImmaginiProperties;
import it.econelsilenzio.immagini.dto.ImmagineDto;
import it.econelsilenzio.immagini.dto.TemaDto;
import it.econelsilenzio.immagini.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unit test: niente Spring, un catalogo finto in una cartella temporanea
class CatalogoServiceTest {

    @TempDir
    Path catalogo;                     // JUnit la crea prima di ogni test e la cancella dopo

    private CatalogoService service;

    @BeforeEach
    void preparaService() {
        ImmaginiProperties props = new ImmaginiProperties(
                new ImmaginiProperties.Catalogo(catalogo.toString()));
        service = new CatalogoService(props, JsonMapper.builder().build());
    }

    // ===================== temi() =====================

    @Test
    @DisplayName("temi: legge tema.json, conta solo le immagini e mette in ordine per 'ordine'")
    void temiInOrdine() throws IOException {
        creaTema("pioggia", """
                { "titolo": "Pioggia", "descrizione": "Gocce sui vetri.", "idealePer": "storie malinconiche", "ordine": 2 }
                """, "01-vetro.webp", "02-strada.webp", "Thumbs.db");
        creaTema("notte", """
                { "titolo": "Notte", "descrizione": "Luna e stelle.", "idealePer": "letture tranquille", "ordine": 1 }
                """, "01-luna.webp", "02-stelle.webp", "03-finestra.webp");

        List<TemaDto> temi = service.temi();

        assertThat(temi).extracting(TemaDto::nome).containsExactly("notte", "pioggia");

        TemaDto notte = temi.getFirst();
        assertThat(notte.titolo()).isEqualTo("Notte");
        assertThat(notte.descrizione()).isEqualTo("Luna e stelle.");
        assertThat(notte.idealePer()).isEqualTo("letture tranquille");
        assertThat(notte.ordine()).isEqualTo(1);
        assertThat(notte.immagini()).isEqualTo(3);

        // tema.json e Thumbs.db non sono immagini
        assertThat(temi.get(1).immagini()).isEqualTo(2);
    }

    @Test
    @DisplayName("temi: un tema senza tema.json funziona con i valori di riserva e va in fondo")
    void temaSenzaJson() throws IOException {
        creaTema("notte", """
                { "titolo": "Notte", "ordine": 1 }
                """, "01-luna.webp");
        creaTema("mare", null, "01-onde.webp");

        List<TemaDto> temi = service.temi();

        assertThat(temi).extracting(TemaDto::nome).containsExactly("notte", "mare");
        TemaDto mare = temi.get(1);
        assertThat(mare.titolo()).isEqualTo("Mare");     // dal nome della cartella, con la maiuscola
        assertThat(mare.descrizione()).isEmpty();
        assertThat(mare.ordine()).isEqualTo(999);
    }

    @Test
    @DisplayName("temi: un tema.json scritto male non fa cadere il catalogo")
    void temaJsonRotto() throws IOException {
        creaTema("bosco", "{ questo non e' JSON", "01-alberi.webp");

        List<TemaDto> temi = service.temi();

        assertThat(temi).hasSize(1);
        assertThat(temi.getFirst().titolo()).isEqualTo("Bosco");
        assertThat(temi.getFirst().immagini()).isEqualTo(1);
    }

    @Test
    @DisplayName("temi: le cartelle con un nome non ammesso vengono ignorate")
    void cartelleConNomeNonAmmesso() throws IOException {
        creaTema("notte", null, "01-luna.webp");
        creaTema("Bozze vecchie", null, "01-prova.webp");   // maiuscola e spazio: non e' un tema

        assertThat(service.temi()).extracting(TemaDto::nome).containsExactly("notte");
    }

    @Test
    @DisplayName("temi: se la cartella del catalogo non esiste, nessun tema (e nessun errore)")
    void catalogoInesistente() {
        ImmaginiProperties props = new ImmaginiProperties(
                new ImmaginiProperties.Catalogo(catalogo.resolve("non-esiste").toString()));
        CatalogoService senzaCatalogo = new CatalogoService(props, JsonMapper.builder().build());

        assertThat(senzaCatalogo.temi()).isEmpty();
    }

    // ===================== immagini(tema) =====================

    @Test
    @DisplayName("immagini: in ordine di nome, ognuna con il suo indirizzo")
    void immaginiDiUnTema() throws IOException {
        creaTema("notte", null, "02-stelle.webp", "01-luna.webp", "tema.json");

        List<ImmagineDto> immagini = service.immagini("notte");

        assertThat(immagini).containsExactly(
                new ImmagineDto("01-luna.webp", "/api/immagini/file/notte/01-luna.webp"),
                new ImmagineDto("02-stelle.webp", "/api/immagini/file/notte/02-stelle.webp"));
    }

    @Test
    @DisplayName("immagini: tema che non esiste → NotFoundException")
    void immaginiTemaInesistente() {
        assertThatThrownBy(() -> service.immagini("deserto"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Tema 'deserto' non trovato");
    }

    // ===================== file(tema, nome) =====================

    @Test
    @DisplayName("file: restituisce il percorso dell'immagine dentro la cartella del tema")
    void fileEsistente() throws IOException {
        creaTema("notte", null, "01-luna.webp");

        Path file = service.file("notte", "01-luna.webp");

        assertThat(file).isRegularFile();
        assertThat(file.getFileName().toString()).isEqualTo("01-luna.webp");
        assertThat(file.getParent().getFileName().toString()).isEqualTo("notte");
    }

    @Test
    @DisplayName("file: immagine che non esiste → NotFoundException")
    void fileInesistente() throws IOException {
        creaTema("notte", null, "01-luna.webp");

        assertThatThrownBy(() -> service.file("notte", "99-sole.webp"))
                .isInstanceOf(NotFoundException.class);
    }

    // ===================== 🔒 Path traversal =====================

    @ParameterizedTest
    @ValueSource(strings = {
            "../segreto.webp",                    // esce dalla cartella del tema (il file ESISTE davvero)
            "..\\segreto.webp",                   // stessa cosa con la barra di Windows
            "../../application.properties",       // prova a leggere la configurazione
            "notte/../../segreto.webp",
            "tema.json",                          // esiste, ma non e' un'immagine
            "virus.exe",                          // estensione non ammessa
            ""
    })
    @DisplayName("🔒 file: nomi pericolosi o non ammessi → sempre NotFoundException")
    void fileNonAmmesso(String nome) throws IOException {
        creaTema("notte", "{}", "01-luna.webp");
        Files.writeString(catalogo.resolve("segreto.webp"), "fuori dal tema");   // l'esca, fuori da "notte"

        assertThatThrownBy(() -> service.file("notte", nome))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Immagine non trovata");      // sempre lo stesso messaggio: non si dice PERCHE'
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "../..", "notte/..", "Notte", "notte ", ""})
    @DisplayName("🔒 tema: nomi pericolosi o non ammessi → NotFoundException")
    void temaNonAmmesso(String tema) throws IOException {
        creaTema("notte", null, "01-luna.webp");

        assertThatThrownBy(() -> service.immagini(tema))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.file(tema, "01-luna.webp"))
                .isInstanceOf(NotFoundException.class);
    }

    // ===================== Metodi di supporto =====================

    // Crea la cartella di un tema con il suo tema.json (se non e' null) e dei file finti
    private void creaTema(String nome, String json, String... file) throws IOException {
        Path cartella = Files.createDirectories(catalogo.resolve(nome));
        if (json != null) {
            Files.writeString(cartella.resolve("tema.json"), json, StandardCharsets.UTF_8);
        }
        for (String f : file) {
            if (!f.equals("tema.json")) {
                Files.write(cartella.resolve(f), new byte[]{1, 2, 3});   // il contenuto non conta
            }
        }
    }
}