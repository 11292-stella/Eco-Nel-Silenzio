import { useEffect, useState } from 'react'
import '../styles/AtmosferaPagina.css'

// Quanto resta ogni immagine prima di sfumare nella successiva
const DURATA_MS = 9000

/**
 * La pagina destra del libro: le immagini di un'atmosfera, una dopo l'altra, in dissolvenza.
 *
 * @param immagini  gli indirizzi delle immagini (dal servizio immagini)
 */
export default function AtmosferaPagina({ immagini }) {
  const [attiva, setAttiva] = useState(0)

  // Ogni DURATA_MS passa all'immagine dopo; dopo l'ultima torna alla prima
  useEffect(() => {
    if (immagini.length < 2) return
    const timer = setInterval(() => {
      setAttiva((i) => (i + 1) % immagini.length)
    }, DURATA_MS)
    return () => clearInterval(timer)   // uscendo dalla pagina il timer si ferma
  }, [immagini])

  // Nessuna immagine (servizio spento o tema vuoto): resta il fregio
  if (immagini.length === 0) {
    return <span className="libro-fregio">❦</span>
  }

  const indice = attiva % immagini.length   // se la lista cambia, l'indice resta valido

  return (
    <div className="atmosfera" data-testid="lettura-atmosfera">
      {immagini.map((src, i) => (
        <img
          key={src}
          src={src}
          alt=""
          className={`atmosfera-immagine ${i === indice ? 'atmosfera-attiva' : ''}`}
          data-testid="lettura-atmosfera-immagine"
        />
      ))}
    </div>
  )
}