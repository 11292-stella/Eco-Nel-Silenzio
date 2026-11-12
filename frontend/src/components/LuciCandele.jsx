import '../styles/LuciCandele.css'

// true = mostra dei cerchi verdi sulle fiamme, per controllare che combacino. In uso normale: false
const PROVA = false

/**
 * Le fiamme dello sfondo (sfondo-lettura.webp, 1376 × 768).
 * x, y = centro della fiamma, in percentuale dell'immagine (0-100).
 * d    = grandezza dell'alone, in percentuale della larghezza dell'immagine.
 * Per spostare una luce: cambia x (destra/sinistra) o y (su/giu').
 */
const CANDELE = [
  // --- a sinistra ---
  { x: 9.8,  y: 8.1,  d: 4 },     // in alto, sulla pila di libri
  { x: 3.1,  y: 32.6, d: 5.5 },   // grande, sul bordo sinistro
  { x: 16.9, y: 44.9, d: 3.5 },   // piccola sul ripiano
  { x: 20.1, y: 41.7, d: 4 },     // accanto, sul ripiano
  { x: 11.4, y: 50.1, d: 5 },     // alta, nel candelabro
  { x: 15.6, y: 57.9, d: 5 },     // nel candelabro d'ottone
  { x: 5.1,  y: 67.4, d: 4.5 },   // piccola, nel portacandela
  { x: 9.4,  y: 76.6, d: 5 },     // davanti, sul tavolo
  // --- al centro ---
  { x: 70.3, y: 42.7, d: 4 },     // sul davanzale della finestra
  // --- a destra ---
  { x: 80.7, y: 10.2, d: 4 },     // in alto, sullo scaffale
  { x: 94.0, y: 44.3, d: 4.5 },   // nella lanterna in alto
  { x: 98.8, y: 44.5, d: 5 },     // sul bordo destro
  { x: 84.3, y: 69.0, d: 5.5 },   // nella lanterna sul tavolo
  { x: 94.3, y: 63.8, d: 5.5 },   // grande, dietro
  { x: 90.8, y: 71.6, d: 5 },     // davanti a sinistra
  { x: 97.7, y: 71.4, d: 5 },     // davanti a destra
  { x: 95.5, y: 75.8, d: 4.5 },   // la piu' bassa
]

/**
 * Le luci delle candele: un alone caldo che tremola sopra ogni fiamma dello sfondo,
 * piu' un leggero respiro di luce su tutta la scena.
 * Il contenitore ha le stesse proporzioni dell'immagine ed e' ritagliato come lo sfondo
 * (cover, in basso al centro): cosi' le luci restano sopra le fiamme su qualsiasi schermo.
 */
export default function LuciCandele() {
  return (
    <div className="luci-candele" aria-hidden="true">
      <div className="luci-immagine">
        {CANDELE.map((candela, i) => (
          <span
            key={i}
            className={`luce ${PROVA ? 'luce-prova' : ''}`}
            style={{
              left: `${candela.x}%`,
              top: `${candela.y}%`,
              '--grandezza': `${candela.d}%`,
              // ogni candela ha il suo ritmo: tremolano in modo diverso, non tutte insieme
              animationDuration: `${2.2 + ((i * 0.37) % 1.6)}s`,
              animationDelay: `-${(i * 0.61) % 3}s`,
            }}
          />
        ))}
      </div>

      {/* il respiro della luce calda su tutta la scena */}
      <div className="luce-ambiente" />
    </div>
  )
}