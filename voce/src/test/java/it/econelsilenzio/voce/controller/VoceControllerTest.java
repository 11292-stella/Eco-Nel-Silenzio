package it.econelsilenzio.voce.controller;

import it.econelsilenzio.voce.service.SintesiException;
import it.econelsilenzio.voce.service.SintesiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Carica solo il controller (+ il gestore degli errori): niente Piper, niente ffmpeg
@WebMvcTest(VoceController.class)
class VoceControllerTest {

    private static final String URL = "/api/voce/sintesi";

    @Autowired
    private MockMvc mockMvc;              // "browser finto" che chiama le API

    @MockitoBean
    private SintesiService sintesiService; // service finto: decidiamo noi cosa risponde

    @TempDir
    Path cartella;                         // cartella temporanea, JUnit la cancella a fine test

    @Test
    @DisplayName("GET con un testo → 200, audio/mpeg, cache di un giorno e il contenuto dell'mp3")
    void sintesiRiuscita() throws Exception {
        byte[] audioFinto = "ID3 audio finto".getBytes();
        Path mp3 = mp3Finto(audioFinto);

        when(sintesiService.sintetizza("C'era una volta un piccolo porto.")).thenReturn(mp3);

        mockMvc.perform(get(URL).param("testo", "C'era una volta un piccolo porto."))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(header().string("Cache-Control", "max-age=86400"))   // 1 giorno in secondi
                .andExpect(content().bytes(audioFinto));
    }

    @Test
    @DisplayName("gli spazi all'inizio e alla fine del testo vengono tolti prima della sintesi")
    void testoConSpazi() throws Exception {
        when(sintesiService.sintetizza("Era una notte buia.")).thenReturn(mp3Finto("audio".getBytes()));

        mockMvc.perform(get(URL).param("testo", "   Era una notte buia.   "))
                .andExpect(status().isOk());

        // il service riceve il testo gia' "pulito": stessa frase = stesso file in cache
        verify(sintesiService).sintetizza("Era una notte buia.");
    }

    @Test
    @DisplayName("GET senza il parametro testo → 400 e il service non viene chiamato")
    void testoMancante() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Manca il testo da leggere"))
                .andExpect(jsonPath("$.dataErrore").exists());

        verifyNoInteractions(sintesiService);
    }

    @Test
    @DisplayName("GET con un testo fatto solo di spazi → 400 (@NotBlank)")
    void testoVuoto() throws Exception {
        mockMvc.perform(get(URL).param("testo", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Il testo deve avere tra 1 e 1000 caratteri"));

        verifyNoInteractions(sintesiService);
    }

    @Test
    @DisplayName("GET con un testo di 1001 caratteri → 400 (@Size)")
    void testoTroppoLungo() throws Exception {
        mockMvc.perform(get(URL).param("testo", "a".repeat(1001)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Il testo deve avere tra 1 e 1000 caratteri"));

        verifyNoInteractions(sintesiService);
    }

    @Test
    @DisplayName("Piper non disponibile → 503: il frontend passera' alla voce del browser")
    void voceNonDisponibile() throws Exception {
        when(sintesiService.sintetizza("Ciao."))
                .thenThrow(new SintesiException("Il server di Piper non e' disponibile"));

        mockMvc.perform(get(URL).param("testo", "Ciao."))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("La voce narrante non è disponibile in questo momento"));
    }

    // Un "mp3" finto nella cartella temporanea: il controller lo restituisce cosi' com'e'
    private Path mp3Finto(byte[] contenuto) throws Exception {
        Path mp3 = cartella.resolve("frase.mp3");
        Files.write(mp3, contenuto);
        return mp3;
    }
}