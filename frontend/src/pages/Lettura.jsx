import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router'
import { paginaLibro } from '../api/libri'
import ControlliLettura from '../components/ControlliLettura'
import { useVoceNarrante } from '../hooks/useVoceNarrante'
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
 * La voce narrante (sigillo di ceralacca) sta nell'hook useVoceNarrante.
 */
export default function Lettura() {
  const { id } = useParams()                  // da "/libri/5" prende "5"

  const [numero, setNumero] = useState(1)     // la pagina del PDF che stiamo leggendo
  const [foglio, setFoglio] = useState(0)     // il foglio dentro la pagina (-1 = l'ultimo)
  const [pagina, setPagina] = useState(null)  // il PaginaDto arrivato dal backend
  const [caricamento, setCaricamento] = useState(true)
  const [errore, setErrore] = useState(null)

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

  // --- La voce narrante: legge il foglio visibile e, finito, gira pagina da sola ---
  const voce = useVoceNarrante({
    frasi: fogli[indice],
    pronto: !caricamento && pagina !== null,
    ceSuccessivo,
    onFineFoglio: avanti,
  })

  // Cambiare pagina del PDF: prepara il caricamento (lo stato lo cambiamo qui, non nell'effetto)
  function cambiaPagina(nuovo, foglioDiArrivo) {
    setCaricamento(true)
    setErrore(null)
    setFoglio(foglioDiArrivo)
    setNumero(nuovo)
  }

  function avanti() {
    voce.azzera()
    if (indice < fogli.length - 1) setFoglio(indice + 1)    // foglio dopo, stessa pagina
    else cambiaPagina(numero + 1, 0)                        // primo foglio della pagina dopo
  }

  function indietro() {
    voce.azzera()
    if (indice > 0) setFoglio(indice - 1)                   // foglio prima, stessa pagina
    else cambiaPagina(numero - 1, -1)                       // ULTIMO foglio della pagina prima
  }

  return (
    <section className="lettura">
      {/* Lo sfondo della scrivania: solo in questa pagina */}
      <div className="lettura-sfondo" aria-hidden="true" />

      {/* Sopra il libro: sigillo (ascolta / pausa) e, piu' avanti, frase precedente / successiva e velocita' */}
      <ControlliLettura voce={voce} disabled={!pagina} />

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