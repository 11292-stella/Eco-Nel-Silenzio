// Tutte le chiamate al servizio libri passano da qui.
// Il percorso e' relativo: in sviluppo il proxy di Vite lo gira al gateway (8080).
const BASE = "/api/libri";

/**
 * Elenco dei libri caricati, dal piu' recente.
 * Restituisce un array di oggetti come il LibroDto di Spring:
 * { id, titolo, nomeFile, numeroPagine, dataCaricamento }
 */
export async function elencoLibri() {
  const risposta = await fetch(BASE);

  // fetch NON lancia errori per 404 o 500: va controllato a mano
  if (!risposta.ok) {
    throw new Error(
      await messaggioErrore(risposta, "Errore nel caricare i libri"),
    );
  }

  return risposta.json();
}

/**
 * Cancella un libro con tutte le sue pagine e frasi.
 * Il backend risponde 204 (nessun corpo) se va bene, 404 se il libro non esiste.
 */
export async function eliminaLibro(id) {
  const risposta = await fetch(`${BASE}/${id}`, { method: "DELETE" });

  if (!risposta.ok) {
    throw new Error(
      await messaggioErrore(risposta, "Errore nell'eliminare il libro"),
    );
  }
  // 204: niente da leggere, quindi niente risposta.json()
}

// Prende il messaggio dall'ApiError del backend ({ status, message, dataErrore }).
// Se il corpo non e' un JSON (es. il gateway e' spento), usa il messaggio di riserva.
async function messaggioErrore(risposta, riserva) {
  try {
    const errore = await risposta.json();
    return errore.message ?? `${riserva} (${risposta.status})`;
  } catch {
    return `${riserva} (${risposta.status})`;
  }
}
