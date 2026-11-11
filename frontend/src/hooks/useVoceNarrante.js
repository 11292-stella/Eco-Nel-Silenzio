import { useEffect, useRef, useState } from "react";
import { urlVoce } from "../api/voce";

// La voce del browser (Web Speech API): la riserva, se il servizio "voce" (Riccardo) non risponde
const VOCE_BROWSER =
  typeof window !== "undefined" && "speechSynthesis" in window;

// Una voce italiana, se il sistema ne ha una
function voceItaliana() {
  return (
    window.speechSynthesis
      .getVoices()
      .find((v) => v.lang.toLowerCase().startsWith("it")) ?? null
  );
}

/**
 * La voce narrante: legge le frasi del foglio una alla volta e dice quale evidenziare.
 * La voce e' Riccardo (servizio voce, Piper); se non risponde si passa alla voce del browser.
 *
 * @param frasi         le frasi del foglio visibile: [{ numero, testo }, ...]
 * @param pronto        false mentre la pagina si sta caricando (la voce aspetta)
 * @param ceSuccessivo  true se dopo questo foglio c'e' altro da leggere
 * @param onFineFoglio  chiamata quando il foglio e' finito (per girare pagina)
 *
 * @returns inAscolto   la voce sta leggendo?
 *          fraseLetta  il numero della frase da evidenziare (o null)
 *          alterna()   ascolta / pausa (il clic sul sigillo)
 *          azzera()    riparte dalla prima frase, senza evidenziazione (cambio di foglio a mano)
 */
export function useVoceNarrante({ frasi, pronto, ceSuccessivo, onFineFoglio }) {
  const [inAscolto, setInAscolto] = useState(false);
  const [fraseLetta, setFraseLetta] = useState(null);
  const daFrase = useRef(0); // da quale frase del foglio ripartire
  const riccardoOk = useRef(true); // false se il servizio voce non ha risposto

  // L'effetto della voce deve poter girare pagina da solo: gli passiamo sempre l'ultima versione della funzione
  const onFineFoglioRef = useRef(onFineFoglio);
  useEffect(() => {
    onFineFoglioRef.current = onFineFoglio;
  });

  // --- La voce legge il foglio, una frase alla volta ---
  useEffect(() => {
    if (!inAscolto || !pronto) return;

    let annullato = false;
    let audioCorrente = null; // l'mp3 che sta suonando
    let prossimo = null; // l'mp3 della frase dopo, scaricato in anticipo

    // Finito il foglio: si gira pagina da soli, oppure ci si ferma alla fine del libro
    function fineFoglio() {
      daFrase.current = 0;
      if (ceSuccessivo) {
        onFineFoglioRef.current();
      } else {
        setInAscolto(false);
        setFraseLetta(null);
      }
    }

    // Riccardo non risponde (servizio spento, 503...): da qui in poi legge la voce del browser
    function passaAlBrowser(i) {
      if (annullato) return;
      console.warn("Voce di Riccardo non disponibile: uso la voce del browser");
      riccardoOk.current = false;
      leggi(i);
    }

    // --- Riccardo: gli mp3 del servizio voce ---
    function leggiConRiccardo(i) {
      const audio =
        prossimo?.indice === i
          ? prossimo.audio
          : new Audio(urlVoce(frasi[i].testo));
      audioCorrente = audio;

      // Intanto prepariamo la frase dopo: quando questa finisce, l'audio e' gia' pronto
      if (i + 1 < frasi.length) {
        const dopo = new Audio(urlVoce(frasi[i + 1].testo));
        dopo.preload = "auto";
        prossimo = { indice: i + 1, audio: dopo };
      } else {
        prossimo = null;
      }

      audio.onplaying = () => {
        if (!annullato) setFraseLetta(frasi[i].numero); // evidenzia la frase
      };
      audio.onended = () => {
        if (!annullato) leggi(i + 1); // e passa alla prossima
      };
      audio.onerror = () => passaAlBrowser(i);

      if (audio.error) {
        passaAlBrowser(i); // era gia' fallito mentre si preparava
        return;
      }
      audio.play().catch(() => {
        if (!annullato) passaAlBrowser(i);
      });
    }

    // --- Riserva: la voce del browser ---
    function leggiConBrowser(i) {
      const voce = new SpeechSynthesisUtterance(frasi[i].testo);
      voce.lang = "it-IT";
      voce.voice = voceItaliana();
      voce.rate = 0.95; // un po' piu' lenta: e' un racconto

      voce.onstart = () => {
        if (!annullato) setFraseLetta(frasi[i].numero);
      };
      voce.onend = () => {
        if (!annullato) leggi(i + 1);
      };
      window.speechSynthesis.speak(voce);
    }

    function leggi(i) {
      if (annullato) return;
      if (i >= frasi.length) {
        fineFoglio();
        return;
      }

      daFrase.current = i;
      if (riccardoOk.current) {
        leggiConRiccardo(i);
      } else if (VOCE_BROWSER) {
        leggiConBrowser(i);
      } else {
        setInAscolto(false); // nessuna voce disponibile
      }
    }

    leggi(daFrase.current);

    // Pausa, cambio di foglio o uscita dalla pagina: la voce si ferma
    return () => {
      annullato = true;
      audioCorrente?.pause();
      if (VOCE_BROWSER) window.speechSynthesis.cancel();
    };
  }, [inAscolto, pronto, frasi, ceSuccessivo]);

  // Clic sul sigillo: ascolta / pausa. In pausa la frase resta evidenziata e si riparte da li'
  function alterna() {
    if (!inAscolto) riccardoOk.current = true; // a ogni "ascolta" si riprova con Riccardo
    setInAscolto((prima) => !prima);
  }

  // Cambio di foglio fatto a mano (Avanti / Indietro): si riparte dalla prima frase
  function azzera() {
    daFrase.current = 0;
    setFraseLetta(null);
  }

  return { inAscolto, fraseLetta, alterna, azzera };
}
