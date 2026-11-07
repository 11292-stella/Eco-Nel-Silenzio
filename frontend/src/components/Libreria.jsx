import { useEffect, useState } from 'react'
import { elencoLibri, eliminaLibro } from '../api/libri'
import CardLibro from './CardLibro'
import ConfermaElimina from './ConfermaElimina'
import '../styles/Libreria.css'
import CorniceAntica from './CorniceAntica'

/**
 * La "libreria": chiede i libri al backend e mostra una CardLibro per ognuno.
 * Gestisce anche l'eliminazione, con la finestra di conferma.
 */
export default function Libreria() {
  const [libri, setLibri] = useState([])          // i libri arrivati dal backend
  const [caricamento, setCaricamento] = useState(true)
  const [errore, setErrore] = useState(null)

  // --- Eliminazione ---
  const [libroDaEliminare, setLibroDaEliminare] = useState(null)   // il libro nella finestra di conferma
  const [eliminazioneInCorso, setEliminazioneInCorso] = useState(false)
  const [erroreEliminazione, setErroreEliminazione] = useState(null)

  // Parte una volta sola, quando il componente compare sulla pagina
  useEffect(() => {
    let attivo = true // evita di aggiornare lo stato se il componente e' gia' sparito

    elencoLibri()
      .then((dati) => {
        if (attivo) setLibri(dati)
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
  }, [])

  // Clic su "Elimina" nella card → apre la finestra di conferma
  function chiediConferma(libro) {
    setErroreEliminazione(null)
    setLibroDaEliminare(libro)
  }

  // "Annulla" o X → chiude la finestra senza fare niente
  function annulla() {
    setLibroDaEliminare(null)
    setErroreEliminazione(null)
  }

  // "Elimina" nella finestra → chiama il backend e toglie il libro dallo scaffale
  async function confermaEliminazione() {
    setEliminazioneInCorso(true)
    setErroreEliminazione(null)

    try {
      await eliminaLibro(libroDaEliminare.id)
      setLibri((attuali) => attuali.filter((l) => l.id !== libroDaEliminare.id))
      setLibroDaEliminare(null)   // chiude la finestra
    } catch (err) {
      setErroreEliminazione(err.message)   // la finestra resta aperta e mostra l'errore
    } finally {
      setEliminazioneInCorso(false)
    }
  }

  // --- Caricamento: spinner di Bootstrap ---
  if (caricamento) {
    return (
      <div className="d-flex align-items-center gap-2" data-testid="libreria-caricamento">
        <div className="spinner-border spinner-border-sm" role="status" />
        <span>Caricamento dei libri...</span>
      </div>
    )
  }

  // --- Errore: avviso rosso ---
  if (errore) {
    return (
      <div className="alert alert-danger" role="alert" data-testid="libreria-errore">
        {errore}
      </div>
    )
  }

  // --- Nessun libro: avviso neutro ---
  if (libri.length === 0) {
    return (
      <div className="alert alert-secondary" data-testid="libreria-vuota">
        Nessun libro caricato.
      </div>
    )
  }

   // --- Lo scaffale: una CardLibro per libro, su un foglio di pergamena ---
  return (
    <CorniceAntica>
      <h2 className="libreria-titolo">La tua libreria</h2>
      <p className="libreria-sottotitolo">Ogni storia aspetta la sua voce</p>
      <span className="libreria-fregio" aria-hidden="true">✦ ❦ ✦</span>

      <div className="row row-cols-2 row-cols-md-3 row-cols-lg-4 gx-4 gy-5" data-testid="libreria-elenco">
        {libri.map((libro) => (
          <div className="col" key={libro.id}>
            <CardLibro libro={libro} onElimina={chiediConferma} />
          </div>
        ))}
      </div>

      <ConfermaElimina
        libro={libroDaEliminare}
        inCorso={eliminazioneInCorso}
        errore={erroreEliminazione}
        onConferma={confermaEliminazione}
        onAnnulla={annulla}
      />
    </CorniceAntica>
  )
}