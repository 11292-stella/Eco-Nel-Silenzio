package it.econelsilenzio.libri.service;

import it.econelsilenzio.libri.dto.PaginaDto;
import it.econelsilenzio.libri.exception.NotFoundException;
import it.econelsilenzio.libri.exception.ValidationException;
import it.econelsilenzio.libri.model.Frase;
import it.econelsilenzio.libri.model.Libro;
import it.econelsilenzio.libri.model.Pagina;
import it.econelsilenzio.libri.repository.LibroRepository;
import it.econelsilenzio.libri.repository.PaginaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;
    private final PaginaRepository paginaRepository;
    private final EstrazioneTestoService estrazioneTestoService;
    private final DivisioneFrasiService divisioneFrasiService;

    /**
     * Legge il PDF, lo divide in pagine e frasi e salva tutto.
     */
    @Transactional
    public Libro carica(String titolo, String nomeFile, byte[] pdf) throws IOException {
        Libro libro = new Libro();
        libro.setTitolo(titoloOppureNomeFile(titolo, nomeFile));
        libro.setNomeFile(nomeFile);

        List<String> testiPagine;
        try {
            testiPagine = estrazioneTestoService.estraiPagine(pdf);
        } catch (IOException e) {
            throw new ValidationException("Il PDF non è leggibile");
        }

        for (int i = 0; i < testiPagine.size(); i++) {
            Pagina pagina = new Pagina();
            pagina.setNumero(i + 1);              // le pagine partono da 1
            pagina.setTesto(testiPagine.get(i));

            List<String> testiFrasi = divisioneFrasiService.dividiInFrasi(testiPagine.get(i));
            for (int j = 0; j < testiFrasi.size(); j++) {
                Frase frase = new Frase();
                frase.setNumero(j + 1);           // anche le frasi partono da 1
                frase.setTesto(testiFrasi.get(j));
                pagina.aggiungiFrase(frase);
            }

            libro.aggiungiPagina(pagina);
        }

        return libroRepository.save(libro);       // salva libro, pagine e frasi insieme
    }

    /**
     * Elenco dei libri, dal più recente.
     */
    @Transactional(readOnly = true)
    public List<Libro> elenco() {
        return libroRepository.findAllByOrderByDataCaricamentoDesc();
    }

    // Se il titolo non è stato scritto, usa il nome del file senza ".pdf"
    private String titoloOppureNomeFile(String titolo, String nomeFile) {
        if (titolo != null && !titolo.isBlank()) {
            return titolo.trim();
        }
        return nomeFile.replaceFirst("(?i)\\.pdf$", "");
    }

    /**
     * Una pagina del libro con le sue frasi, pronta per il frontend.
     * La conversione in DTO avviene qui, dentro la transazione.
     */
    @Transactional(readOnly = true)
    public PaginaDto pagina(Long libroId, int numero) {
        return paginaRepository.findByLibroIdAndNumero(libroId, numero)
                .map(PaginaDto::da)
                .orElseThrow(() -> new NotFoundException(
                        "Pagina " + numero + " del libro " + libroId + " non trovata"));
    }
}