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

/**
 * Carica un PDF: il backend lo divide in pagine e frasi e lo salva.
 * Risponde 201 con il libro creato (come il LibroDto), 400 se il file non va bene.
 *
 * @param file    il file scelto dall'utente (un oggetto File dell'<input type="file">)
 * @param titolo  facoltativo: se e' vuoto, il backend usa il nome del file
 */
export async function caricaLibro(file, titolo) {
  // FormData = il "multipart/form-data" che il controller si aspetta (@RequestParam "file" e "titolo")
  const dati = new FormData();
  dati.append("file", file);
  if (titolo && titolo.trim() !== "") {
    dati.append("titolo", titolo.trim());
  }

  // NON mettere l'header Content-Type: lo scrive il browser, con il "boundary" giusto
  const risposta = await fetch(BASE, { method: "POST", body: dati });

  if (!risposta.ok) {
    throw new Error(
      await messaggioErrore(risposta, "Errore nel caricare il libro"),
    );
  }

  return risposta.json();
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
