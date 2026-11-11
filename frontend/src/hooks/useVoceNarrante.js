import { useEffect, useRef, useState } from "react";
import { urlVoce } from "../api/voce";

// La voce del browser (Web Speech API): la riserva, se il servizio "voce" (Riccardo) non risponde
const VOCE_BROWSER =
  typeof window !== "undefined" && "speechSynthesis" in window;

// Le velocita' disponibili (1 = normale)
export const VELOCITA = [0.8, 1, 1.15, 1.3];
const CHIAVE_VELOCITA = "eco-velocita";

// Una voce italiana, se il sistema ne ha una
function voceItaliana() {
  return (
    window.speechSynthesis
      .getVoices()
      .find((v) => v.lang.toLowerCase().startsWith("it")) ?? null
  );
}

// La velocita' salvata nel browser; se manca o e' strana, 1
function leggiVelocita() {
  try {
    const salvata = Number(localStorage.getItem(CHIAVE_VELOCITA));
    return VELOCITA.includes(salvata) ? salvata : 1;
  } catch {
    return 1;
  }
}

function salvaVelocita(valore) {
  try {
    localStorage.setItem(CHIAVE_VELOCITA, String(valore));
  } catch {
    // storage non disponibile: la velocita' vale solo finche' la pagina resta aperta
  }
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
 * @returns inAscolto          la voce sta leggendo?
 *          fraseLetta         il numero della frase da evidenziare (o null)
 *          alterna()          ascolta / pausa (il clic sul sigillo)
 *          azzera()           riparte dalla prima frase, senza evidenziazione (cambio di foglio a mano)
 *          velocita           la velocita' attuale (es. 1.15)
 *          prossimaVelocita() passa alla velocita' successiva della lista (dopo l'ultima torna alla prima)
 *          fraseSuccessiva()  frase dopo (all'ultima del foglio gira pagina)
 *          frasePrecedente()  frase prima (sulla prima la ricomincia)
 */
export function useVoceNarrante({ frasi, pronto, ceSuccessivo, onFineFoglio }) {
  const [inAscolto, setInAscolto] = useState(false);
  const [fraseLetta, setFraseLetta] = useState(null);
  const [velocita, setVelocita] = useState(leggiVelocita);
  // Cambia a ogni salto di frase (⏮ ⏭): e' tra le dipendenze dell'effetto, cosi' la lettura riparte dalla frase nuova
  const [giro, setGiro] = useState(0);
  const daFrase = useRef(0); // da quale frase del foglio ripartire
  const riccardoOk = useRef(true); // false se il servizio voce non ha risposto

  // La velocita' la legge l'effetto della voce, ma cambiarla NON deve far ripartire la lettura:
  // per questo sta anche in un ref, che l'effetto legge senza averlo tra le dipendenze
  const velocitaRef = useRef(velocita);
  const audioRef = useRef(null); // l'mp3 che sta suonando, per cambiargli la velocita' al volo

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
      audioRef.current = audio;
      // piu' veloce o piu' lento, ma con la stessa tonalita' (il browser la mantiene da solo)
      audio.playbackRate = velocitaRef.current;

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
      // 0.95 = un po' piu' lenta, da racconto; la velocita' scelta vale dalla frase successiva
      voce.rate = 0.95 * velocitaRef.current;

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

    // Pausa, cambio di foglio, salto di frase o uscita dalla pagina: la voce si ferma
    return () => {
      annullato = true;
      audioCorrente?.pause();
      audioRef.current = null;
      if (VOCE_BROWSER) window.speechSynthesis.cancel();
    };
  }, [inAscolto, pronto, frasi, ceSuccessivo, giro]);

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

  // Nuova velocita': vale SUBITO per Riccardo (anche a meta' frase), dalla frase dopo per la voce del browser
  function cambiaVelocita(valore) {
    setVelocita(valore);
    velocitaRef.current = valore;
    salvaVelocita(valore);
    if (audioRef.current) audioRef.current.playbackRate = valore;
  }

  // Clic sul pulsante della velocita': 0.8 → 1 → 1.15 → 1.3 → di nuovo 0.8
  function prossimaVelocita() {
    const posizione = VELOCITA.indexOf(velocita);
    cambiaVelocita(VELOCITA[(posizione + 1) % VELOCITA.length]);
  }

  // Salta a una frase del foglio: la illumina e, se la voce sta leggendo, riparte da li'
  function vaiAFrase(i) {
    daFrase.current = i;
    setFraseLetta(frasi[i].numero);
    setGiro((g) => g + 1);
  }

  // ⏭ Frase dopo; all'ultima del foglio si gira pagina (come quando la voce finisce il foglio)
  function fraseSuccessiva() {
    if (frasi.length === 0) return;
    const dopo = daFrase.current + 1;
    if (dopo < frasi.length) {
      vaiAFrase(dopo);
    } else if (ceSuccessivo) {
      onFineFoglioRef.current();
    }
  }

  // ⏮ Frase prima; sulla prima frase del foglio la ricomincia
  function frasePrecedente() {
    if (frasi.length === 0) return;
    vaiAFrase(Math.max(daFrase.current - 1, 0));
  }

  return {
    inAscolto,
    fraseLetta,
    alterna,
    azzera,
    velocita,
    prossimaVelocita,
    fraseSuccessiva,
    frasePrecedente,
  };
}
