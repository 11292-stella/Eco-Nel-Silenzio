// Le chiamate al servizio immagini (il catalogo delle atmosfere).
// Il percorso e' relativo: in sviluppo il proxy di Vite lo gira al gateway (8080), che lo manda a immagini (8083).
const BASE = "/api/immagini";

/**
 * Tutti i temi, in ordine.
 * [{ nome, titolo, descrizione, idealePer, ordine, immagini }, ...]
 */
export async function elencoTemi() {
  const risposta = await fetch(`${BASE}/temi`);
  if (!risposta.ok) {
    throw new Error(`Errore ${risposta.status} nel caricare i temi`);
  }
  return risposta.json();
}

/**
 * Le immagini di un tema, ognuna con il suo indirizzo.
 * [{ nome: "01-luna-sul-porto.webp", url: "/api/immagini/file/notte/01-luna-sul-porto.webp" }, ...]
 */
export async function immaginiTema(tema) {
  const risposta = await fetch(`${BASE}/temi/${encodeURIComponent(tema)}`);
  if (!risposta.ok) {
    throw new Error(`Errore ${risposta.status} nel caricare il tema ${tema}`);
  }
  return risposta.json();
}
