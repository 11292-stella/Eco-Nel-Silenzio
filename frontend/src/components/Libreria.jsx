import { useEffect, useState } from 'react'
import { elencoLibri } from '../api/libri'

/**
 * La "libreria": mostra l'elenco dei libri caricati.
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

  if (caricamento) {
    return <p data-testid="libreria-caricamento">Caricamento dei libri...</p>
  }

  if (errore) {
    return <p data-testid="libreria-errore">{errore}</p>
  }

  if (libri.length === 0) {
    return <p data-testid="libreria-vuota">Nessun libro caricato.</p>
  }

  return (
    <section>
      <h2>La tua libreria</h2>
      <ul data-testid="libreria-elenco">
        {libri.map((libro) => (
          <li key={libro.id} data-testid="libro">
            <strong>{libro.titolo}</strong>
            {' · '}
            {libro.numeroPagine} pagine
            {' · '}
            caricato il {new Date(libro.dataCaricamento).toLocaleDateString('it-IT')}
          </li>
        ))}
      </ul>
    </section>
  )
}