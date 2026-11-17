import { useEffect, useState } from "react";
import { immaginiTema } from "../api/immagini";

/**
 * Gli indirizzi delle immagini di un tema, pronti per <img src>.
 * Se il servizio immagini non risponde restituisce [] e la pagina mostra il fregio:
 * il libro si legge lo stesso (degradazione controllata, come per la voce).
 */
export function useAtmosfera(tema) {
  const [immagini, setImmagini] = useState([]);

  useEffect(() => {
    let attivo = true;

    immaginiTema(tema)
      .then((dati) => {
        if (attivo) setImmagini(dati.map((immagine) => immagine.url));
      })
      .catch((err) => {
        console.warn("Atmosfera non disponibile:", err.message);
        if (attivo) setImmagini([]);
      });

    return () => {
      attivo = false;
    };
  }, [tema]);

  return immagini;
}
