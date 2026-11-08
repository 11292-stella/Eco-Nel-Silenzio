import '../styles/SigilloVoce.css'

// Il bordo irregolare della goccia di cera (disegnato una volta, con un po' di colature)
const BORDO_CERA =
  'M96.6 50.0 L98.2 52.5 L96.9 54.9 L94.7 57.1 L94.1 59.4 L93.9 61.8 L92.2 63.7 L90.3 65.5 L90.0 67.8 L90.1 70.4 L88.1 72.0 L85.1 72.8 L83.5 74.3 L83.2 76.9 L82.6 79.3 L81.8 81.8 L82.1 85.6 L82.7 90.4 L81.5 93.3 L78.3 93.5 L75.0 93.2 L72.2 93.6 L69.1 93.0 L65.7 91.0 L63.1 90.2 L61.2 91.7 L59.1 93.0 L56.7 92.5 L54.4 91.9 L52.2 92.7 L50.0 93.0 L47.8 91.5 L45.8 90.3 L43.4 91.6 L40.7 93.7 L38.1 94.3 L35.7 94.0 L32.9 94.6 L30.1 94.7 L28.5 92.2 L27.8 88.4 L26.6 86.0 L24.7 84.8 L23.4 82.9 L22.5 80.5 L20.5 79.5 L17.6 79.1 L15.9 77.6 L15.8 74.9 L15.0 72.8 L12.7 71.5 L10.9 69.9 L10.0 67.8 L8.3 66.0 L4.9 64.6 L2.7 62.7 L3.4 59.9 L4.8 57.2 L5.2 54.7 L6.1 52.3 L8.6 50.0 L10.3 47.9 L9.4 45.7 L7.8 43.3 L7.8 41.0 L8.3 38.8 L7.1 36.0 L5.4 32.9 L5.8 30.3 L7.7 28.4 L8.7 26.2 L9.2 23.5 L10.9 21.6 L13.5 20.4 L15.2 18.7 L16.7 16.7 L19.6 16.3 L23.6 17.4 L26.4 17.5 L27.9 15.9 L29.4 14.4 L31.5 13.6 L33.0 11.8 L34.1 8.5 L35.8 6.4 L38.4 6.6 L40.8 6.9 L43.0 5.6 L45.2 4.4 L47.6 4.8 L50.0 5.0 L52.4 3.6 L55.0 2.7 L57.2 4.7 L59.0 7.7 L60.9 9.3 L62.9 10.2 L64.7 11.8 L66.7 12.6 L69.8 11.2 L73.2 9.9 L75.3 11.0 L76.7 13.2 L78.8 14.4 L81.4 15.1 L82.8 17.2 L83.0 20.3 L84.3 22.2 L86.9 23.2 L88.8 24.8 L88.9 27.5 L89.3 30.0 L90.4 32.0 L90.4 34.5 L88.6 37.5 L87.7 39.9 L89.5 41.6 L91.7 43.4 L92.7 45.5 L94.0 47.7 Z'

/**
 * Il sigillo di ceralacca sul libro: avvia e mette in pausa la voce narrante.
 *
 * @param inAscolto  true mentre la voce sta leggendo (mostra "pausa")
 * @param disabled   true se la voce non e' disponibile o la pagina si sta caricando
 * @param onClick    chiamata al clic
 */
export default function SigilloVoce({ inAscolto, disabled, onClick }) {
  const etichetta = inAscolto ? 'pausa' : 'ascolta'

  return (
    <button
      type="button"
      className={`sigillo ${inAscolto ? 'in-ascolto' : ''}`}
      onClick={onClick}
      disabled={disabled}
      aria-label={inAscolto ? 'Metti in pausa la voce' : 'Ascolta la voce narrante'}
      aria-pressed={inAscolto}
      data-testid="sigillo-voce"
    >
      {/* la cera */}
      <svg className="sigillo-cera" viewBox="0 0 100 100" aria-hidden="true">
        <defs>
          <radialGradient id="cera" cx="38%" cy="32%" r="75%">
            <stop offset="0%" stopColor="#d9604f" />
            <stop offset="45%" stopColor="#a1281b" />
            <stop offset="100%" stopColor="#5e110c" />
          </radialGradient>
        </defs>
        <path d={BORDO_CERA} fill="url(#cera)" />
        {/* il cerchio impresso dal timbro */}
        <circle cx="50" cy="50" r="31" fill="none" stroke="#4a0906" strokeOpacity="0.45" strokeWidth="2.2" />
        <circle cx="50" cy="51" r="31" fill="none" stroke="#ffc2ae" strokeOpacity="0.25" strokeWidth="1" />
        <circle cx="50" cy="50" r="27" fill="#000" fillOpacity="0.08" />
      </svg>

      {/* l'icona impressa: due barre (pausa) oppure un triangolo (ascolta) */}
      <svg className="sigillo-icona" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        {inAscolto ? (
          <>
            <rect x="5" y="4" width="5" height="16" rx="1" />
            <rect x="14" y="4" width="5" height="16" rx="1" />
          </>
        ) : (
          <path d="M7 4.5v15a1 1 0 0 0 1.5.86l12-7.5a1 1 0 0 0 0-1.72l-12-7.5A1 1 0 0 0 7 4.5z" />
        )}
      </svg>

      <span className="sigillo-etichetta" aria-hidden="true">{etichetta}</span>
    </button>
  )
}