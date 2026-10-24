package it.econelsilenzio.libri.controller;

import it.econelsilenzio.libri.dto.PaginaDto;
import it.econelsilenzio.libri.exception.NotFoundException;
import it.econelsilenzio.libri.model.Libro;
import it.econelsilenzio.libri.service.LibroService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Carica solo il controller (+ l'handler delle eccezioni), niente database
@WebMvcTest(LibroController.class)
class LibroControllerTest {

    @Autowired
    private MockMvc mockMvc;          // "browser finto" che chiama le API

    @MockitoBean
    private LibroService libroService; // service finto: decidiamo noi cosa risponde

    @Test
    @DisplayName("POST /api/libri con un PDF → 201 e il riepilogo del libro")
    void caricaPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "racconto.pdf", "application/pdf", "contenuto finto".getBytes());

        when(libroService.carica(eq("Rosso Malpelo"), eq("racconto.pdf"), any()))
                .thenReturn(libroFinto(1L, "Rosso Malpelo", 12));

        mockMvc.perform(multipart("/api/libri")
                        .file(file)
                        .param("titolo", "Rosso Malpelo"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titolo").value("Rosso Malpelo"))
                .andExpect(jsonPath("$.numeroPagine").value(12));
    }

    @Test
    @DisplayName("POST /api/libri con file vuoto → 400 e il service non viene chiamato")
    void fileVuoto() throws Exception {
        MockMultipartFile vuoto = new MockMultipartFile(
                "file", "racconto.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/libri").file(vuoto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Il file è vuoto"))
                .andExpect(jsonPath("$.dataErrore").exists());

        verifyNoInteractions(libroService);
    }

    @Test
    @DisplayName("POST /api/libri con un file non PDF → 400")
    void fileNonPdf() throws Exception {
        MockMultipartFile testo = new MockMultipartFile(
                "file", "appunti.txt", "text/plain", "ciao".getBytes());

        mockMvc.perform(multipart("/api/libri").file(testo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Il file deve essere un PDF"));

        verifyNoInteractions(libroService);
    }

    @Test
    @DisplayName("GET di una pagina esistente → 200 con le frasi in ordine")
    void paginaEsistente() throws Exception {
        PaginaDto pagina = new PaginaDto(1L, 3, 42, List.of(
                new PaginaDto.FraseDto(1, "Era una notte buia."),
                new PaginaDto.FraseDto(2, "Il sig. Rossi tornò a casa!")));

        when(libroService.pagina(1L, 3)).thenReturn(pagina);

        mockMvc.perform(get("/api/libri/1/pagine/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(3))
                .andExpect(jsonPath("$.totalePagine").value(42))
                .andExpect(jsonPath("$.frasi.length()").value(2))
                .andExpect(jsonPath("$.frasi[1].testo").value("Il sig. Rossi tornò a casa!"));
    }

    @Test
    @DisplayName("GET di una pagina inesistente → 404 con ApiError")
    void paginaInesistente() throws Exception {
        when(libroService.pagina(1L, 99))
                .thenThrow(new NotFoundException("Pagina 99 del libro 1 non trovata"));

        mockMvc.perform(get("/api/libri/1/pagine/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Pagina 99 del libro 1 non trovata"));
    }

    // Un Libro già "salvato", come lo restituirebbe il service vero
    private Libro libroFinto(Long id, String titolo, int numeroPagine) {
        Libro libro = new Libro();
        libro.setId(id);
        libro.setTitolo(titolo);
        libro.setNomeFile("racconto.pdf");
        libro.setNumeroPagine(numeroPagine);
        libro.setDataCaricamento(LocalDateTime.now());
        return libro;
    }
}