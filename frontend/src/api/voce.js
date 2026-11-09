// Le chiamate al servizio voce (Piper, la voce di Riccardo).
// Il percorso e' relativo: in sviluppo il proxy di Vite lo gira al gateway (8080), che lo manda a voce (8082).
const BASE = "/api/voce";

/**
 * L'indirizzo dell'mp3 di una frase.
 * Non serve fetch: lo si da' direttamente a new Audio(...) e il browser scarica e suona.
 * encodeURIComponent trasforma spazi, accenti e « » in caratteri sicuri per un indirizzo.
 */
export function urlVoce(testo) {
  return `${BASE}/sintesi?testo=${encodeURIComponent(testo)}`;
}
