import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { paginaLibro } from '../api/libri'
import { urlVoce } from '../api/voce'
import SigilloVoce from '../components/SigilloVoce'
import '../styles/Lettura.css'

// Quanti caratteri stanno, piu' o meno, nella pagina disegnata del libro.
// Se il testo esce dalla carta, abbassa questo numero; se avanza spazio, alzalo.
const CARATTERI_PER_FOGLIO = 650

// La voce del browser (Web Speech API): la riserva, se il servizio "voce" (Riccardo) non risponde
const VOCE_BROWSER = typeof window !== 'undefined' && 'speechSynthesis' in window

/**
 * Divide le frasi di una pagina del PDF in "fogli" che stanno nella pagina del libro.
 * Una frase non viene mai spezzata a meta'.
 */
function dividiInFogli(frasi) {
  const fogli = []
  let corrente = []
  let lunghezza = 0

  for (const frase of frasi) {
    const l = frase.testo.length + 1
    if (corrente.length > 0 && lunghezza + l > CARATTERI_PER_FOGLIO) {
      fogli.push(corrente)
      corrente = []
      lunghezza = 0
    }
    corrente.push(frase)
    lunghezza += l
  }
  if (corrente.length > 0) fogli.push(corrente)

  return fogli.length > 0 ? fogli : [[]]
}

// Una voce italiana, se il sistema ne ha una
function voceItaliana() {
  return window.speechSynthesis.getVoices().find((v) => v.lang.toLowerCase().startsWith('it')) ?? null
}

/**
 * La pagina di lettura di un libro: /libri/:id
 * Il libro aperto: a sinistra il testo, a destra (piu' avanti) le immagini d'atmosfera.
 * Il sigillo di ceralacca (sopra il libro) avvia e ferma la voce, che legge frase per frase e la evidenzia.
 * La voce e' Riccardo (servizio voce, Piper); se non risponde si passa alla voce del browser.
 */
export default function Lettura() {
  const { id } = useParams()                  // da "/libri/5" prende "5"

  const [numero, setNumero] = useState(1)     // la pagina del PDF che stiamo leggendo
  const [foglio, setFoglio] = useState(0)     // il foglio dentro la pagina (-1 = l'ultimo)
  const [pagina, setPagina] = useState(null)  // il PaginaDto arrivato dal backend
  const [caricamento, setCaricamento] = useState(true)
  const [errore, setErrore] = useState(null)

  // --- Voce ---
  const [inAscolto, setInAscolto] = useState(false)   // la voce sta leggendo?
  const [fraseLetta, setFraseLetta] = useState(null)  // il numero della frase evidenziata
  const daFrase = useRef(0)                           // da quale frase del foglio ripartire
  const riccardoOk = useRef(true)                     // false se il servizio voce non ha risposto

  // Riparte ogni volta che cambia il libro (id) o la pagina (numero).
  // Qui dentro si aggiorna lo stato solo con la risposta del server (then / catch / finally).
  useEffect(() => {
    let attivo = true

    paginaLibro(id, numero)
      .then((dati) => {
        if (attivo) setPagina(dati)
      })
      .catch((err) => {
        if (attivo) setErrore(err.message)
      })
      .finally(() => {
        if (attivo) setCaricamento(false)
      })

    return () => {
      attivo = false
    }
  }, [id, numero])

  // I fogli si ricalcolano solo quando arriva una pagina nuova
  const fogli = useMemo(() => dividiInFogli(pagina?.frasi ?? []), [pagina])
  const totale = pagina?.totalePagine ?? 1
  const indice = foglio === -1 ? fogli.length - 1 : Math.min(foglio, fogli.length - 1)

  const cePrecedente = indice > 0 || numero > 1
  const ceSuccessivo = indice < fogli.length - 1 || numero < totale

  // Cambiare pagina del PDF: prepara il caricamento (lo stato lo cambiamo qui, non nell'effetto)
  function cambiaPagina(nuovo, foglioDiArrivo) {
    setCaricamento(true)
    setErrore(null)
    setFoglio(foglioDiArrivo)
    setNumero(nuovo)
  }

  function avanti() {
    daFrase.current = 0
    setFraseLetta(null)
    if (indice < fogli.length - 1) setFoglio(indice + 1)    // foglio dopo, stessa pagina
    else cambiaPagina(numero + 1, 0)                        // primo foglio della pagina dopo
  }

  function indietro() {
    daFrase.current = 0
    setFraseLetta(null)
    if (indice > 0) setFoglio(indice - 1)                   // foglio prima, stessa pagina
    else cambiaPagina(numero - 1, -1)                       // ULTIMO foglio della pagina prima
  }

  // L'effetto della voce deve poter girare pagina da solo: gli passiamo sempre l'ultima versione di "avanti"
  const avantiRef = useRef(avanti)
  useEffect(() => {
    avantiRef.current = avanti
  })

  // --- La voce legge il foglio, una frase alla volta ---
  useEffect(() => {
    if (!inAscolto || caricamento || !pagina) return

    const frasi = fogli[indice]
    let annullato = false
    let audioCorrente = null   // l'mp3 che sta suonando
    let prossimo = null        // l'mp3 della frase dopo, scaricato in anticipo

    // Finito il foglio: si gira pagina da soli, oppure ci si ferma alla fine del libro
    function fineFoglio() {
      daFrase.current = 0
      if (ceSuccessivo) {
        avantiRef.current()
      } else {
        setInAscolto(false)
        setFraseLetta(null)
      }
    }

    // Riccardo non risponde (servizio spento, 503...): da qui in poi legge la voce del browser
    function passaAlBrowser(i) {
      if (annullato) return
      console.warn('Voce di Riccardo non disponibile: uso la voce del browser')
      riccardoOk.current = false
      leggi(i)
    }

    // --- Riccardo: gli mp3 del servizio voce ---
    function leggiConRiccardo(i) {
      const audio = prossimo?.indice === i ? prossimo.audio : new Audio(urlVoce(frasi[i].testo))
      audioCorrente = audio

      // Intanto prepariamo la frase dopo: quando questa finisce, l'audio e' gia' pronto
      if (i + 1 < frasi.length) {
        const dopo = new Audio(urlVoce(frasi[i + 1].testo))
        dopo.preload = 'auto'
        prossimo = { indice: i + 1, audio: dopo }
      } else {
        prossimo = null
      }

      audio.onplaying = () => {
        if (!annullato) setFraseLetta(frasi[i].numero)   // evidenzia la frase
      }
      audio.onended = () => {
        if (!annullato) leggi(i + 1)                     // e passa alla prossima
      }
      audio.onerror = () => passaAlBrowser(i)

      if (audio.error) {
        passaAlBrowser(i)                                // era gia' fallito mentre si preparava
        return
      }
      audio.play().catch(() => {
        if (!annullato) passaAlBrowser(i)
      })
    }

    // --- Riserva: la voce del browser ---
    function leggiConBrowser(i) {
      const voce = new SpeechSynthesisUtterance(frasi[i].testo)
      voce.lang = 'it-IT'
      voce.voice = voceItaliana()
      voce.rate = 0.95                                     // un po' piu' lenta: e' un racconto

      voce.onstart = () => {
        if (!annullato) setFraseLetta(frasi[i].numero)
      }
      voce.onend = () => {
        if (!annullato) leggi(i + 1)
      }
      window.speechSynthesis.speak(voce)
    }

    function leggi(i) {
      if (annullato) return
      if (i >= frasi.length) {
        fineFoglio()
        return
      }

      daFrase.current = i
      if (riccardoOk.current) {
        leggiConRiccardo(i)
      } else if (VOCE_BROWSER) {
        leggiConBrowser(i)
      } else {
        setInAscolto(false)                                // nessuna voce disponibile
      }
    }

    leggi(daFrase.current)

    // Pausa, cambio pagina o uscita dalla pagina: la voce si ferma
    return () => {
      annullato = true
      audioCorrente?.pause()
      if (VOCE_BROWSER) window.speechSynthesis.cancel()
    }
  }, [inAscolto, caricamento, pagina, indice, fogli, ceSuccessivo])

  // Clic sul sigillo: ascolta / pausa. In pausa la frase resta evidenziata e si riparte da li'
  function alternaVoce() {
    if (!inAscolto) riccardoOk.current = true   // a ogni "ascolta" si riprova con Riccardo
    setInAscolto((prima) => !prima)
  }

  return (
    <section className="lettura">
      {/* Lo sfondo della scrivania: solo in questa pagina */}
      <div className="lettura-sfondo" aria-hidden="true" />

      <div className="lettura-intestazione">
        {/* Il sigillo di ceralacca: ascolta / pausa */}
        <SigilloVoce
          inAscolto={inAscolto}
          disabled={!pagina}
          onClick={alternaVoce}
        />
      </div>

      {errore && (
        <div className="alert alert-danger text-center" role="alert" data-testid="lettura-errore">
          {errore}
        </div>
      )}

      <div className="libro-scena" data-testid="lettura-libro">
        {/* Pagina sinistra: il testo */}
        <article
          className="libro-pagina libro-pagina-sx"
          lang="it"
          aria-busy={caricamento}
          data-testid="lettura-pagina"
        >
          {!pagina ? (
            !errore && <p className="fst-italic">Sto aprendo il libro...</p>
          ) : (
            <p className="mb-0">
              {fogli[indice].map((frase) => (
                <span
                  key={frase.numero}
                  className={`frase ${frase.numero === fraseLetta ? 'frase-letta' : ''}`}
                  data-testid="lettura-frase"
                >
                  {frase.testo}{' '}
                </span>
              ))}
            </p>
          )}
        </article>

        {/* Pagina destra: qui arriveranno le immagini d'atmosfera */}
        <div className="libro-pagina libro-pagina-dx" aria-hidden="true">
          <span className="libro-fregio">❦</span>
        </div>
      </div>

      <div className="lettura-navigazione">
        <button
          type="button"
          className="btn btn-leggi"
          onClick={indietro}
          disabled={caricamento || !cePrecedente}
          data-testid="lettura-precedente"
        >
          ← Indietro
        </button>

        <div className="lettura-centro">
          {pagina && (
            <span className="lettura-numero" data-testid="lettura-numero">
              Pagina {numero} di {totale}
            </span>
          )}
          <Link to="/" className="btn-elimina" data-testid="lettura-libreria">
            torna alla libreria
          </Link>
        </div>

        <button
          type="button"
          className="btn btn-leggi"
          onClick={avanti}
          disabled={caricamento || !ceSuccessivo}
          data-testid="lettura-successiva"
        >
          Avanti →
        </button>
      </div>
    </section>
  )
}