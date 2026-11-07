import '../styles/CardLibro.css'

// Numero di colori di copertina definiti in CardLibro.css (.copertina-0 ... .copertina-3)
const COLORI_COPERTINA = 4

/**
 * Un libro sullo scaffale: copertina di cuoio, mensola di legno e cartellino.
 *
 * @param libro      un oggetto come il LibroDto: { id, titolo, numeroPagine, dataCaricamento }
 * @param onElimina  chiamata quando si preme "elimina" (riceve il libro)
 */
export default function CardLibro({ libro, onElimina }) {
  const colore = libro.id % COLORI_COPERTINA
  const titolo = titoloLeggibile(libro.titolo)
  const data = new Date(libro.dataCaricamento).toLocaleDateString('it-IT')

  return (
    <div className="libro" data-testid="card-libro">
      {/* Copertina */}
      <div className={`copertina copertina-${colore}`}>
        
        <div className="copertina-contenuto">
          <span className="copertina-fregio">❦</span>
          <h5 className="copertina-titolo" data-testid="card-libro-titolo">{titolo}</h5>
          <span className="copertina-fregio">✦</span>
        </div>
      </div>

      {/* Mensola */}
      <div className="mensola" />

      {/* Cartellino */}
      <div className="cartellino">
        <div className="cartellino-info">
          {libro.numeroPagine} pagine · {data}
        </div>
        {/* Per ora "#": con React Router portera' alla pagina di lettura /libri/{id} */}
        <a href="#" className="btn btn-leggi btn-sm" data-testid="card-libro-leggi">
          Leggi
        </a>{' '}
        <button
          type="button"
          className="btn-elimina"
          onClick={() => onElimina(libro)}
          aria-label={`Elimina ${titolo}`}
          data-testid="card-libro-elimina"
        >
          elimina
        </button>
      </div>
    </div>
  )
}

// "La_lanterna_del_porto_vecchio" → "La lanterna del porto vecchio"
function titoloLeggibile(titolo) {
  return titolo.replaceAll('_', ' ')
}