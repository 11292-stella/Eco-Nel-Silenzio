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
    throw new Error(`Errore ${risposta.status} nel caricare i libri`);
  }

  return risposta.json();
}
