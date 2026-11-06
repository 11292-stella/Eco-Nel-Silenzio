import '../styles/CardLibro.css'

// Numero di colori di copertina definiti in CardLibro.css (.copertina-0 ... .copertina-3)
const COLORI_COPERTINA = 4

/**
 * Card di un libro, disegnata come una copertina.
 * Base: la "card" classica di Bootstrap 5.3, con la copertina al posto di card-img-top.
 *
 * @param libro un oggetto come il LibroDto: { id, titolo, numeroPagine, dataCaricamento }
 */
export default function CardLibro({ libro }) {
  const colore = libro.id % COLORI_COPERTINA
  const data = new Date(libro.dataCaricamento).toLocaleDateString('it-IT')

  return (
    <div className="card card-libro h-100" data-testid="card-libro">
      {/* Copertina */}
      <div className={`copertina copertina-${colore}`}>
        <div>
          <span className="copertina-fregio">✦ ✦ ✦</span>
          <h5 className="copertina-titolo" data-testid="card-libro-titolo">
            {titoloLeggibile(libro.titolo)}
          </h5>
          <span className="copertina-fregio">✦ ✦ ✦</span>
        </div>
      </div>

      {/* Informazioni e pulsante */}
      <div className="card-body d-flex flex-column">
        <p className="card-text text-body-secondary small mb-3">
          {libro.numeroPagine} pagine · caricato il {data}
        </p>
        {/* Per ora "#": con React Router portera' alla pagina di lettura /libri/{id} */}
        <a href="#" className="btn btn-leggi btn-sm mt-auto" data-testid="card-libro-leggi">
          Leggi
        </a>
      </div>
    </div>
  )
}

// "La_lanterna_del_porto_vecchio" → "La lanterna del porto vecchio"
function titoloLeggibile(titolo) {
  return titolo.replaceAll('_', ' ')
}