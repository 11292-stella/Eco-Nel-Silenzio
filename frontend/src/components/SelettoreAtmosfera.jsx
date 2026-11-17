import { useEffect, useState } from 'react'
import { elencoTemi } from '../api/immagini'
import { TEMA_PREDEFINITO } from '../utils/atmosfera'
import '../styles/SelettoreAtmosfera.css'

/**
 * Le etichette dei temi d'atmosfera, nella plancia.
 * I temi arrivano dal servizio immagini: se e' spento, il selettore non compare.
 */
export default function SelettoreAtmosfera({ temaAttivo, onCambia }) {
  const [temi, setTemi] = useState([])

  // L'elenco dei temi si carica una volta sola, quando si apre il libro
  useEffect(() => {
    let attivo = true
    elencoTemi()
      .then((dati) => { if (attivo) setTemi(dati) })
      .catch((err) => console.warn('Temi non disponibili:', err.message))
    return () => { attivo = false }
  }, [])

  // Il tema salvato non esiste piu' nel catalogo (cartella cancellata o rinominata):
  // si torna al predefinito, cosi' sulla pagina non resta il fregio
  useEffect(() => {
    if (temi.length > 0 && !temi.some((t) => t.nome === temaAttivo)) {
      onCambia(TEMA_PREDEFINITO)
    }
  }, [temi, temaAttivo, onCambia])

  if (temi.length === 0) return null

  return (
    <div className="atmosfere" role="group" aria-label="Atmosfera" data-testid="selettore-atmosfera">
      {temi.map((t) => (
        <button
          key={t.nome}
          type="button"
          className={`etichetta-atmosfera ${t.nome === temaAttivo ? 'etichetta-attiva' : ''}`}
          aria-pressed={t.nome === temaAttivo}
          title={`${t.descrizione} Ideale per ${t.idealePer}.`}
          onClick={() => onCambia(t.nome)}
          data-testid={`atmosfera-${t.nome}`}
        >
          {t.titolo}
        </button>
      ))}
    </div>
  )
}