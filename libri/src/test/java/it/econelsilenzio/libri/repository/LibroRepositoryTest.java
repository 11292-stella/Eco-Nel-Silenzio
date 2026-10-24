package it.econelsilenzio.libri.repository;


import it.econelsilenzio.libri.model.Frase;
import it.econelsilenzio.libri.model.Libro;
import it.econelsilenzio.libri.model.Pagina;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import it.econelsilenzio.libri.TestcontainersConfiguration;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Test di integrazione: solo la parte JPA di Spring + un PostgreSQL vero in Docker
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class LibroRepositoryTest {

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private PaginaRepository paginaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("salva il libro con pagine e frasi a catena (e le migration Flyway funzionano)")
    void salvaLibroConPagineEFrasi() {
        Libro salvato = libroRepository.save(libroDiProva("Rosso Malpelo", 2));
        svuotaCache();

        Libro letto = libroRepository.findById(salvato.getId()).orElseThrow();

        assertThat(letto.getTitolo()).isEqualTo("Rosso Malpelo");
        assertThat(letto.getNumeroPagine()).isEqualTo(2);
        assertThat(letto.getDataCaricamento()).isNotNull();   // messa da @PrePersist
        assertThat(letto.getPagine()).hasSize(2);
    }

    @Test
    @DisplayName("l'elenco mette i libri dal più recente al più vecchio")
    void elencoDalPiuRecente() {
        libroRepository.save(libroDiProva("Primo", 1));
        libroRepository.save(libroDiProva("Secondo", 1));
        svuotaCache();

        List<Libro> libri = libroRepository.findAllByOrderByDataCaricamentoDesc();

        assertThat(libri)
                .extracting(Libro::getTitolo)
                .containsExactly("Secondo", "Primo");
    }

    @Test
    @DisplayName("trova la pagina giusta con libro e frasi già caricati (@EntityGraph)")
    void trovaPaginaConFrasi() {
        Libro salvato = libroRepository.save(libroDiProva("Racconto", 3));
        svuotaCache();

        Pagina pagina = paginaRepository
                .findByLibroIdAndNumero(salvato.getId(), 2)
                .orElseThrow();

        // Stacco la pagina dal database: da qui il caricamento LAZY non funziona più.
        // Se libro e frasi non fossero già caricati, le righe sotto darebbero
        // LazyInitializationException → così il test controlla davvero l'@EntityGraph.
        entityManager.clear();

        assertThat(pagina.getNumero()).isEqualTo(2);
        assertThat(pagina.getLibro().getTitolo()).isEqualTo("Racconto");
        assertThat(pagina.getFrasi())
                .extracting(Frase::getTesto)
                .containsExactly("Frase 1 della pagina 2.", "Frase 2 della pagina 2.");
    }

    @Test
    @DisplayName("pagina inesistente → Optional vuoto")
    void paginaInesistente() {
        Libro salvato = libroRepository.save(libroDiProva("Corto", 1));
        svuotaCache();

        Optional<Pagina> risultato = paginaRepository.findByLibroIdAndNumero(salvato.getId(), 99);

        assertThat(risultato).isEmpty();
    }

    // --- metodi di supporto ---

    // Un libro finto con N pagine, ognuna con 2 frasi
    private Libro libroDiProva(String titolo, int numeroPagine) {
        Libro libro = new Libro();
        libro.setTitolo(titolo);
        libro.setNomeFile(titolo + ".pdf");

        for (int n = 1; n <= numeroPagine; n++) {
            Pagina pagina = new Pagina();
            pagina.setNumero(n);
            pagina.setTesto("Testo della pagina " + n + ".");

            for (int f = 1; f <= 2; f++) {
                Frase frase = new Frase();
                frase.setNumero(f);
                frase.setTesto("Frase " + f + " della pagina " + n + ".");
                pagina.aggiungiFrase(frase);
            }
            libro.aggiungiPagina(pagina);
        }
        return libro;
    }

    // Scrive tutto nel database e svuota la "memoria" di Hibernate:
    // così le letture successive vanno davvero su PostgreSQL
    private void svuotaCache() {
        entityManager.flush();
        entityManager.clear();
    }
}