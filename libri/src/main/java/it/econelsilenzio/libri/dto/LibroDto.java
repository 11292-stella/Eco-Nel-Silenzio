package it.econelsilenzio.libri.dto;

import it.econelsilenzio.libri.model.Libro;

import java.time.LocalDateTime;

/**
 * Riepilogo di un libro: quello che serve per l'elenco,
 * senza pagine e frasi.
 */
public record LibroDto(
        Long id,
        String titolo,
        String nomeFile,
        int numeroPagine,
        LocalDateTime dataCaricamento
) {

    public static LibroDto da(Libro libro) {
        return new LibroDto(
                libro.getId(),
                libro.getTitolo(),
                libro.getNomeFile(),
                libro.getNumeroPagine(),
                libro.getDataCaricamento()
        );
    }
}