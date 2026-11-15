package it.econelsilenzio.immagini.dto;

/**
 * Un tema del catalogo (una cartella), per l'elenco dei temi.
 * Esempio JSON:
 * { "nome": "notte", "titolo": "Notte", "descrizione": "...", "idealePer": "...", "ordine": 1, "immagini": 4 }
 *
 * @param nome         il nome della cartella: si usa negli indirizzi (/api/immagini/temi/notte)
 * @param titolo       il nome da mostrare ("Notte", "Città")
 * @param descrizione  cosa c'e' nelle immagini (servira' anche a Ollama per scegliere)
 * @param idealePer    per quali storie e' adatto
 * @param ordine       la posizione nel selettore (1 = primo)
 * @param immagini     quante immagini ci sono nella cartella
 */
public record TemaDto(
        String nome,
        String titolo,
        String descrizione,
        String idealePer,
        int ordine,
        int immagini
) {}
