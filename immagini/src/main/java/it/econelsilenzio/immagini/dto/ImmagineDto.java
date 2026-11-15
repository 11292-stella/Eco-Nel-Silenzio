package it.econelsilenzio.immagini.dto;

/**
 * Un'immagine di un tema, con l'indirizzo per scaricarla.
 * Esempio JSON:
 * { "nome": "01-luna-sul-porto.webp", "url": "/api/immagini/file/notte/01-luna-sul-porto.webp" }
 *
 * @param nome  il nome del file
 * @param url   l'indirizzo da dare a <img src="..."> nel frontend
 */
public record ImmagineDto(
        String nome,
        String url
) {}