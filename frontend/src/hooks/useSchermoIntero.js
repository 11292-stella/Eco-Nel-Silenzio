import { useEffect, useState } from "react";

/**
 * Lo schermo intero del browser (Fullscreen API).
 *
 * @returns supportato  false dove lo schermo intero per le pagine non esiste (es. Safari su iPhone)
 *          attivo      true se siamo a schermo intero
 *          alterna()   entra / esce
 *
 * Uscendo dalla pagina che lo usa (es. "torna alla libreria"), si esce anche dallo schermo intero.
 */
export function useSchermoIntero() {
  const supportato =
    typeof document !== "undefined" && document.fullscreenEnabled === true;
  const [attivo, setAttivo] = useState(
    () => supportato && document.fullscreenElement !== null,
  );

  useEffect(() => {
    if (!supportato) return;

    // Si puo' uscire anche con Esc o dal browser: l'evento ci tiene sempre allineati
    function aggiorna() {
      setAttivo(document.fullscreenElement !== null);
    }
    document.addEventListener("fullscreenchange", aggiorna);

    return () => {
      document.removeEventListener("fullscreenchange", aggiorna);
      // Uscendo dalla pagina: niente schermo intero "dimenticato" (la navbar tornerebbe nascosta)
      if (document.fullscreenElement) {
        document.exitFullscreen().catch(() => {});
      }
    };
  }, [supportato]);

  function alterna() {
    if (!supportato) return;
    // Le funzioni restituiscono una Promise: se il browser rifiuta, non facciamo niente
    if (document.fullscreenElement) {
      document.exitFullscreen().catch(() => {});
    } else {
      document.documentElement.requestFullscreen().catch(() => {});
    }
  }

  return { supportato, attivo, alterna };
}
