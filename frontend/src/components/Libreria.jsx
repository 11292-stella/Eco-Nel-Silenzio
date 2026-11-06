import { useEffect, useState } from 'react'
import { elencoLibri } from '../api/libri'
import CardLibro from './CardLibro'

/**
 * La "libreria": chiede i libri al backend e mostra una CardLibro per ognuno.
 */
export default function Libreria() {
  const [libri, setLibri] = useState([])          // i libri arrivati dal backend
  const [caricamento, setCaricamento] = useState(true)
  const [errore, setErrore] = useState(null)

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

  // --- Lo scaffale: una CardLibro per libro ---
  return (
    <section>
      <h2 className="mb-4">La tua libreria</h2>

      <div className="row row-cols-2 row-cols-md-3 row-cols-lg-4 g-4" data-testid="libreria-elenco">
        {libri.map((libro) => (
          <div className="col" key={libro.id}>
            <CardLibro libro={libro} />
          </div>
        ))}
      </div>
    </section>
  )
}