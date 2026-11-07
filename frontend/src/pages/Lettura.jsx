import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { paginaLibro } from '../api/libri'
import CorniceAntica from '../components/CorniceAntica'
import '../styles/Lettura.css'

/**
 * La pagina di lettura di un libro: /libri/:id
 * Chiede al backend una pagina alla volta e permette di sfogliare avanti e indietro.
 */
export default function Lettura() {
  const { id } = useParams()                  // da "/libri/5" prende "5"

  const [numero, setNumero] = useState(1)     // la pagina che stiamo leggendo
  const [pagina, setPagina] = useState(null)  // il PaginaDto arrivato dal backend
  const [caricamento, setCaricamento] = useState(true)
  const [errore, setErrore] = useState(null)

  // Riparte ogni volta che cambia il libro (id) o la pagina (numero).
  // Qui dentro si aggiorna lo stato solo con la risposta del server (then / catch / finally).
  useEffect(() => {
    let attivo = true

    paginaLibro(id, numero)
      .then((dati) => {
        if (attivo) setPagina(dati)
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
  }, [id, numero])

  // Clic su precedente/successiva: prepara il caricamento e cambia pagina
  function vaiAPagina(nuovo) {
    setCaricamento(true)
    setErrore(null)
    setNumero(nuovo)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const totale = pagina?.totalePagine ?? 1

  return (
    <CorniceAntica>
      <div className="lettura-intestazione">
        <h2 className="libreria-titolo">Eco nel Silenzio</h2>
        {pagina && (
          <span className="lettura-numero" data-testid="lettura-numero">
            Pagina {numero} di {totale}
          </span>
        )}
      </div>

      {errore ? (
        <div className="alert alert-danger" role="alert" data-testid="lettura-errore">
          {errore}
        </div>
      ) : (
        <article className="pagina-libro" data-testid="lettura-pagina" aria-busy={caricamento}>
          {caricamento && !pagina ? (
            <p className="text-center fst-italic">Sto aprendo il libro...</p>
          ) : (
            <p className="mb-0">
              {pagina.frasi.map((frase) => (
                <span key={frase.numero} className="frase" data-testid="lettura-frase">
                  {frase.testo}{' '}
                </span>
              ))}
            </p>
          )}
        </article>
      )}

      <div className="lettura-navigazione">
        <button
          type="button"
          className="btn btn-leggi"
          onClick={() => vaiAPagina(numero - 1)}
          disabled={caricamento || numero <= 1}
          data-testid="lettura-precedente"
        >
          ← Pagina precedente
        </button>

        <Link to="/" className="btn-elimina" data-testid="lettura-libreria">
          torna alla libreria
        </Link>

        <button
          type="button"
          className="btn btn-leggi"
          onClick={() => vaiAPagina(numero + 1)}
          disabled={caricamento || numero >= totale}
          data-testid="lettura-successiva"
        >
          Pagina successiva →
        </button>
      </div>
    </CorniceAntica>
  )
}