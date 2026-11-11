import { useState } from 'react'
import { Link } from 'react-router'
import SigilloVoce from './SigilloVoce'
import { useInattivita } from '../hooks/useInattivita'
import '../styles/ControlliLettura.css'

/**
 * La plancia di lettura: galleggia in basso, un po' sopra il libro, con tutti i comandi.
 * Mentre la voce legge e nessuno tocca niente per 3 secondi, sparisce; al primo movimento riappare.
 *
 *   [◀ Indietro]   (⏮)  (SIGILLO)  (⏭)  [1×]   [Avanti ▶]
 *        ━━━━━━━━━━━━━●──────────   Pagina 1 di 3
 *          ↶ torna alla libreria     ⟲ ricomincia
 *
 * @param voce              quello che restituisce useVoceNarrante
 * @param pronta            false finche' la pagina non e' arrivata
 * @param caricamento       true mentre arriva una pagina nuova (sfogliare e' bloccato)
 * @param numero, totale    pagina attuale e numero di pagine
 * @param avanzamento       quanto si e' letto, tra 0 e 1
 * @param cePrecedente      c'e' qualcosa prima?
 * @param ceSuccessivo      c'e' qualcosa dopo?
 * @param onIndietro, onAvanti, onRicomincia   le azioni della pagina
 */
export default function ControlliLettura({
  voce, pronta, caricamento,
  numero, totale, avanzamento,
  cePrecedente, ceSuccessivo,
  onIndietro, onAvanti, onRicomincia,
}) {
  // 1.15 → "1,15×" (con la virgola, all'italiana)
  const etichettaVelocita = `${voce.velocita.toLocaleString('it-IT')}×`
  const percentuale = Math.round(avanzamento * 100)
  const allInizio = !cePrecedente

  // La plancia sparisce solo mentre la voce legge, se nessuno tocca niente da 3 secondi
  // e il mouse non e' sopra la plancia
  const inattivo = useInattivita(3000)
  const [mouseSopra, setMouseSopra] = useState(false)
  const nascosta = voce.inAscolto && inattivo && !mouseSopra

  return (
    <div
      className={`plancia ${nascosta ? 'plancia-nascosta' : ''}`}
      onMouseEnter={() => setMouseSopra(true)}
      onMouseLeave={() => setMouseSopra(false)}
      data-testid="lettura-controlli"
    >
      {/* --- Riga 1: sfogliare e la voce --- */}
      <div className="plancia-comandi">
        <button
          type="button"
          className="btn-cuoio"
          onClick={onIndietro}
          disabled={caricamento || !cePrecedente}
          data-testid="lettura-precedente"
        >
          ◀ Indietro
        </button>

        <div className="plancia-voce">
          <button
            type="button"
            className="borchia"
            onClick={voce.frasePrecedente}
            disabled={!pronta}
            aria-label="Frase precedente"
            title="Frase precedente"
            data-testid="lettura-frase-precedente"
          >
            <IconaSalto verso="indietro" />
          </button>

          <SigilloVoce
            inAscolto={voce.inAscolto}
            disabled={!pronta}
            onClick={voce.alterna}
          />

          <button
            type="button"
            className="borchia"
            onClick={voce.fraseSuccessiva}
            disabled={!pronta}
            aria-label="Frase successiva"
            title="Frase successiva"
            data-testid="lettura-frase-successiva"
          >
            <IconaSalto verso="avanti" />
          </button>

          {/* Velocita': a ogni clic la successiva (0,8× → 1× → 1,15× → 1,3×) */}
          <button
            type="button"
            className="controllo-velocita"
            onClick={voce.prossimaVelocita}
            aria-label={`Velocità di lettura: ${etichettaVelocita}. Clicca per cambiarla`}
            title="Velocità di lettura"
            data-testid="lettura-velocita"
          >
            {etichettaVelocita}
          </button>
        </div>

        <button
          type="button"
          className="btn-cuoio"
          onClick={onAvanti}
          disabled={caricamento || !ceSuccessivo}
          data-testid="lettura-successiva"
        >
          Avanti ▶
        </button>
      </div>

      {/* --- Riga 2: il nastro di avanzamento e il numero di pagina --- */}
      <div className="plancia-avanzamento">
        <div
          className="nastro"
          role="progressbar"
          aria-label="Avanzamento della lettura"
          aria-valuenow={percentuale}
          aria-valuemin="0"
          aria-valuemax="100"
          data-testid="lettura-avanzamento"
        >
          <div className="nastro-pieno" style={{ width: `${percentuale}%` }} />
        </div>
        {pronta && (
          <span className="plancia-numero" data-testid="lettura-numero">
            Pagina {numero} di {totale}
          </span>
        )}
      </div>

      {/* --- Riga 3: uscire o ricominciare --- */}
      <div className="plancia-collegamenti">
        <Link to="/" className="collegamento-inchiostro" data-testid="lettura-libreria">
          ↶ torna alla libreria
        </Link>
        {!allInizio && (
          <button
            type="button"
            className="collegamento-inchiostro"
            onClick={onRicomincia}
            disabled={caricamento}
            data-testid="lettura-ricomincia"
          >
            ⟲ ricomincia
          </button>
        )}
      </div>
    </div>
  )
}

// Le frecce di ⏮ ⏭ disegnate in SVG: su Windows i caratteri ⏮ ⏭ diventerebbero emoji colorate
function IconaSalto({ verso }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width="18"
      height="18"
      aria-hidden="true"
      style={verso === 'indietro' ? { transform: 'scaleX(-1)' } : undefined}
    >
      <path d="M5 5 L15 12 L5 19 Z" fill="currentColor" />
      <rect x="16" y="5" width="3" height="14" rx="1" fill="currentColor" />
    </svg>
  )
}