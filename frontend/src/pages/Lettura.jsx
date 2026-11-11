import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router'
import { paginaLibro } from '../api/libri'
import ControlliLettura from '../components/ControlliLettura'
import { useVoceNarrante } from '../hooks/useVoceNarrante'
import { leggiSegnalibro, salvaSegnalibro } from '../utils/segnalibro'
import '../styles/Lettura.css'

// Quanti caratteri stanno, piu' o meno, nella pagina disegnata del libro.
// Se il testo esce dalla carta, abbassa questo numero; se avanza spazio, alzalo.
const CARATTERI_PER_FOGLIO = 650

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

/**
 * La pagina di lettura di un libro: /libri/:id
 * Il libro aperto: a sinistra il testo, a destra (piu' avanti) le immagini d'atmosfera.
 * Sotto il libro la plancia con tutti i comandi; la voce sta nell'hook useVoceNarrante.
 * Sfogliando, un foglio di pergamena gira sulla rilegatura.
 */
export default function Lettura() {
  const { id } = useParams()                  // da "/libri/5" prende "5"

  // Si riparte dal segnalibro (se c'e'): la funzione dentro useState gira solo la prima volta
  const [numero, setNumero] = useState(() => leggiSegnalibro(id).numero)   // la pagina del PDF
  const [foglio, setFoglio] = useState(() => leggiSegnalibro(id).foglio)   // il foglio (-1 = l'ultimo)
  const [pagina, setPagina] = useState(null)  // il PaginaDto arrivato dal backend
  const [caricamento, setCaricamento] = useState(true)
  const [errore, setErrore] = useState(null)

  // Il foglio che gira: { verso: 'avanti' | 'indietro', chiave } oppure null.
  // La chiave cambia a ogni giro, cosi' React crea un elemento nuovo e l'animazione riparte da capo
  const [sfoglio, setSfoglio] = useState(null)

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

  // Quanto si e' letto, tra 0 e 1: le pagine gia' finite + i fogli letti di quella attuale
  const avanzamento = pagina
    ? Math.min(((numero - 1) + (indice + 1) / fogli.length) / totale, 1)
    : 0

  // Ogni volta che cambia il foglio visibile, aggiorna il segnalibro
  useEffect(() => {
    if (pagina) salvaSegnalibro(id, numero, indice)
  }, [id, numero, indice, pagina])

  // --- La voce narrante: legge il foglio visibile e, finito, gira pagina da sola ---
  const voce = useVoceNarrante({
    frasi: fogli[indice],
    pronto: !caricamento && pagina !== null,
    ceSuccessivo,
    onFineFoglio: avanti,
  })

  // Fa partire l'animazione del foglio che gira
  function sfoglia(verso) {
    setSfoglio((prima) => ({ verso, chiave: (prima?.chiave ?? 0) + 1 }))
  }

  // Cambiare pagina del PDF: prepara il caricamento (lo stato lo cambiamo qui, non nell'effetto)
  function cambiaPagina(nuovo, foglioDiArrivo) {
    setCaricamento(true)
    setErrore(null)
    setFoglio(foglioDiArrivo)
    setNumero(nuovo)
  }

  function avanti() {
    voce.azzera()
    sfoglia('avanti')
    if (indice < fogli.length - 1) setFoglio(indice + 1)    // foglio dopo, stessa pagina
    else cambiaPagina(numero + 1, 0)                        // primo foglio della pagina dopo
  }

  function indietro() {
    voce.azzera()
    sfoglia('indietro')
    if (indice > 0) setFoglio(indice - 1)                   // foglio prima, stessa pagina
    else cambiaPagina(numero - 1, -1)                       // ULTIMO foglio della pagina prima
  }

  // "Ricomincia dall'inizio": prima pagina, primo foglio
  function ricomincia() {
    voce.azzera()
    sfoglia('indietro')
    if (numero === 1) setFoglio(0)
    else cambiaPagina(1, 0)
  }

  return (
    <section className="lettura">
      {/* Lo sfondo della scrivania: solo in questa pagina */}
      <div className="lettura-sfondo" aria-hidden="true" />

      {errore && (
        <div className="alert alert-danger text-center" role="alert" data-testid="lettura-errore">
          {errore}
        </div>
      )}

      <div className="libro-scena" data-testid="lettura-libro">
        {/* Pagina sinistra: il testo.
            La key cambia a ogni foglio: il testo nuovo "compare" mentre il foglio che gira atterra */}
        <article
          key={`${numero}-${indice}`}
          className="libro-pagina libro-pagina-sx testo-compare"
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
                  className={`frase ${frase.numero === voce.fraseLetta ? 'frase-letta' : ''}`}
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

        {/* Il foglio che gira sulla rilegatura: c'e' solo durante l'animazione, poi si toglie da solo */}
        {sfoglio && (
          <div
            key={sfoglio.chiave}
            className={`foglio-che-gira foglio-${sfoglio.verso}`}
            onAnimationEnd={() => setSfoglio(null)}
            aria-hidden="true"
            data-testid="lettura-foglio-che-gira"
          />
        )}
      </div>

      {/* Sotto il libro: la plancia con tutti i comandi */}
      <ControlliLettura
        voce={voce}
        pronta={pagina !== null}
        caricamento={caricamento}
        numero={numero}
        totale={totale}
        avanzamento={avanzamento}
        cePrecedente={cePrecedente}
        ceSuccessivo={ceSuccessivo}
        onIndietro={indietro}
        onAvanti={avanti}
        onRicomincia={ricomincia}
      />
    </section>
  )
}