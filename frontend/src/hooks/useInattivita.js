import { useEffect, useState } from "react";

// Gli eventi che contano come "l'utente sta facendo qualcosa"
const EVENTI = ["mousemove", "mousedown", "touchstart", "keydown", "wheel"];

/**
 * true quando l'utente non tocca niente da "millisecondi" (mouse fermo, nessun tasto, nessun tocco).
 * Al primo movimento torna subito false.
 * Serve per far sparire la plancia mentre si ascolta, come i comandi dei lettori video.
 */
export function useInattivita(millisecondi = 3000) {
  const [inattivo, setInattivo] = useState(false);

  useEffect(() => {
    let timer = setTimeout(() => setInattivo(true), millisecondi);

    // Ogni movimento: "sveglio" e il conto alla rovescia riparte
    function attivita() {
      setInattivo(false); // se era gia' false, React non ridisegna niente
      clearTimeout(timer);
      timer = setTimeout(() => setInattivo(true), millisecondi);
    }

    EVENTI.forEach((evento) =>
      window.addEventListener(evento, attivita, { passive: true }),
    );

    // Uscendo dalla pagina: via il timer e gli ascoltatori
    return () => {
      clearTimeout(timer);
      EVENTI.forEach((evento) => window.removeEventListener(evento, attivita));
    };
  }, [millisecondi]);

  return inattivo;
}
