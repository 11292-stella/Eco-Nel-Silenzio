package it.econelsilenzio.libri.dto;

import it.econelsilenzio.libri.model.Frase;
import it.econelsilenzio.libri.model.Pagina;

import java.util.List;

/**
 * Una pagina del libro con le sue frasi, in ordine.
 * È quello che il frontend mostra sulla pagina destra.
 */
public record PaginaDto(
        Long libroId,
        int numero,
        int totalePagine,
        List<FraseDto> frasi
) {

    /** Una singola frase: l'unità che la voce legge ed evidenzia. */
    public record FraseDto(int numero, String testo) {

        public static FraseDto da(Frase frase) {
            return new FraseDto(frase.getNumero(), frase.getTesto());
        }
    }

    public static PaginaDto da(Pagina pagina) {
        return new PaginaDto(
                pagina.getLibro().getId(),
                pagina.getNumero(),
                pagina.getLibro().getNumeroPagine(),
                pagina.getFrasi().stream()
                        .map(FraseDto::da)
                        .toList()
        );
    }
}