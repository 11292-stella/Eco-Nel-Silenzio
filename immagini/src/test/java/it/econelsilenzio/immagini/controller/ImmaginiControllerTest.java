package it.econelsilenzio.immagini.controller;

import it.econelsilenzio.immagini.dto.ImmagineDto;
import it.econelsilenzio.immagini.dto.TemaDto;
import it.econelsilenzio.immagini.exception.NotFoundException;
import it.econelsilenzio.immagini.service.CatalogoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Solo il controller + GestoreErrori; il service e' finto
@WebMvcTest(ImmaginiController.class)
class ImmaginiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogoService catalogoService;

    @TempDir
    Path cartella;

    // ===================== /temi =====================

    @Test
    @DisplayName("GET /temi → 200 con i temi in JSON")
    void temi() throws Exception {
        when(catalogoService.temi()).thenReturn(List.of(
                new TemaDto("notte", "Notte", "Luna e stelle.", "letture tranquille", 1, 4)));

        mockMvc.perform(get("/api/immagini/temi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("notte"))
                .andExpect(jsonPath("$[0].titolo").value("Notte"))
                .andExpect(jsonPath("$[0].idealePer").value("letture tranquille"))
                .andExpect(jsonPath("$[0].immagini").value(4));
    }

    // ===================== /temi/{tema} =====================

    @Test
    @DisplayName("GET /temi/notte → 200 con le immagini e il loro indirizzo")
    void immaginiDiUnTema() throws Exception {
        when(catalogoService.immagini("notte")).thenReturn(List.of(
                new ImmagineDto("01-luna.webp", "/api/immagini/file/notte/01-luna.webp")));

        mockMvc.perform(get("/api/immagini/temi/notte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("01-luna.webp"))
                .andExpect(jsonPath("$[0].url").value("/api/immagini/file/notte/01-luna.webp"));
    }

    @Test
    @DisplayName("GET /temi/deserto → 404 con ApiError")
    void temaInesistente() throws Exception {
        when(catalogoService.immagini("deserto"))
                .thenThrow(new NotFoundException("Tema 'deserto' non trovato"));

        mockMvc.perform(get("/api/immagini/temi/deserto"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Tema 'deserto' non trovato"))
                .andExpect(jsonPath("$.dataErrore").exists());
    }

    // ===================== /file/{tema}/{nome} =====================

    @Test
    @DisplayName("GET /file/notte/01-luna.webp → 200, image/webp, cache di un giorno e il contenuto esatto")
    void fileImmagine() throws Exception {
        byte[] contenuto = {10, 20, 30, 40};
        Path file = Files.write(cartella.resolve("01-luna.webp"), contenuto);
        when(catalogoService.file("notte", "01-luna.webp")).thenReturn(file);

        mockMvc.perform(get("/api/immagini/file/notte/01-luna.webp"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/webp"))
                .andExpect(header().string("Cache-Control", "max-age=86400"))
                .andExpect(content().bytes(contenuto));
    }

    @Test
    @DisplayName("GET di un file che non esiste → 404 'Immagine non trovata'")
    void fileInesistente() throws Exception {
        when(catalogoService.file("notte", "99-sole.webp"))
                .thenThrow(new NotFoundException("Immagine non trovata"));

        mockMvc.perform(get("/api/immagini/file/notte/99-sole.webp"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Immagine non trovata"));
    }

    // ===================== 🔒 Path traversal e indirizzi sbagliati =====================

    @Test
    @DisplayName("🔒 ..%2F..%2Fapplication.properties: arriva decodificato al service, che lo blocca → 404")
    void pathTraversalCodificato() throws Exception {
        when(catalogoService.file(eq("notte"), any()))
                .thenThrow(new NotFoundException("Immagine non trovata"));

        // URI.create: l'indirizzo parte ESATTAMENTE cosi', con le / codificate (%2F)
        mockMvc.perform(get(URI.create("/api/immagini/file/notte/..%2F..%2Fapplication.properties")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Immagine non trovata"));

        // Il controller ha passato il nome DECODIFICATO: il controllo vero lo fa il service (Passo 91)
        verify(catalogoService).file("notte", "../../application.properties");
    }

    @Test
    @DisplayName("🔒 /file/notte/../../pom.xml con le / vere: nessuna API corrisponde → 404, service mai chiamato")
    void pathTraversalConLeBarre() throws Exception {
        mockMvc.perform(get(URI.create("/api/immagini/file/notte/../../pom.xml")))
                .andExpect(status().isNotFound());

        verifyNoInteractions(catalogoService);
    }

    @Test
    @DisplayName("GET /file/notte senza il nome → 404 'Indirizzo non trovato' (non 500)")
    void indirizzoIncompleto() throws Exception {
        mockMvc.perform(get("/api/immagini/file/notte"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Indirizzo non trovato"));

        verifyNoInteractions(catalogoService);
    }

    // ===================== Il tipo del file =====================

    @ParameterizedTest
    @CsvSource({
            "luna.webp,  image/webp",
            "nebbia.webm, video/webm",
            "onde.png,   image/png",
            "bosco.jpg,  image/jpeg",
            "MARE.JPEG,  image/jpeg"
    })
    @DisplayName("il tipo del file si decide dall'estensione (anche in maiuscolo)")
    void tipoDelFile(String nome, String atteso) {
        assertThat(ImmaginiController.tipo(nome).toString()).isEqualTo(atteso);
    }
}