import { useRef, useState } from 'react'
import { caricaLibro } from '../api/libri'
import '../styles/CaricaLibro.css'

// Stesso limite del backend (spring.servlet.multipart.max-file-size=50MB)
const MAX_MB = 50

/**
 * Pulsante "Carica un libro" che apre un modulo per mandare un PDF al backend.
 *
 * @param onCaricato  chiamata con il libro creato (il LibroDto) quando il caricamento riesce
 */
export default function CaricaLibro({ onCaricato }) {
  const [aperto, setAperto] = useState(false)
  const [file, setFile] = useState(null)
  const [titolo, setTitolo] = useState('')
  const [inCorso, setInCorso] = useState(false)
  const [errore, setErrore] = useState(null)

  const inputFile = useRef(null)   // per svuotare il campo del file dopo il caricamento

  // Controlli prima di mandare il file: il backend li rifa' comunque, ma cosi' l'utente lo sa subito
  function controllaFile(scelto) {
    if (!scelto) return 'Scegli un file PDF'
    if (!scelto.name.toLowerCase().endsWith('.pdf')) return 'Il file deve essere un PDF'
    if (scelto.size > MAX_MB * 1024 * 1024) return `Il file supera i ${MAX_MB} MB`
    return null
  }

  function scegliFile(evento) {
    const scelto = evento.target.files[0] ?? null
    setFile(scelto)
    setErrore(scelto ? controllaFile(scelto) : null)
  }

  async function invia(evento) {
    evento.preventDefault()   // il form non deve ricaricare la pagina

    const problema = controllaFile(file)
    if (problema) {
      setErrore(problema)
      return
    }

    setInCorso(true)
    setErrore(null)

    try {
      const libro = await caricaLibro(file, titolo)
      onCaricato(libro)        // avvisa la libreria: il libro va sullo scaffale
      chiudi()
    } catch (err) {
      setErrore(err.message)   // es. "Il file deve essere un PDF" dalla ValidationException
    } finally {
      setInCorso(false)
    }
  }

  function chiudi() {
    setAperto(false)
    setFile(null)
    setTitolo('')
    setErrore(null)
    if (inputFile.current) inputFile.current.value = ''
  }

  return (
    <div className="carica-libro">
      {!aperto && (
        <button
          type="button"
          className="btn btn-apri-carica"
          onClick={() => setAperto(true)}
          data-testid="carica-apri"
        >
          ✒ Carica un libro
        </button>
      )}

      {aperto && (
        <form className="foglietto" onSubmit={invia} data-testid="carica-form">
          <h3 className="foglietto-titolo">Un nuovo libro per lo scaffale</h3>

          <div className="mb-3">
            <label htmlFor="carica-file" className="form-label">Il racconto in PDF</label>
            <input
              id="carica-file"
              ref={inputFile}
              type="file"
              accept="application/pdf,.pdf"
              className="form-control"
              onChange={scegliFile}
              disabled={inCorso}
              data-testid="carica-file"
            />
            <div className="form-text">Massimo {MAX_MB} MB.</div>
          </div>

          <div className="mb-3">
            <label htmlFor="carica-titolo" className="form-label">Titolo (facoltativo)</label>
            <input
              id="carica-titolo"
              type="text"
              className="form-control"
              placeholder="Se lo lasci vuoto, uso il nome del file"
              value={titolo}
              onChange={(e) => setTitolo(e.target.value)}
              disabled={inCorso}
              data-testid="carica-titolo"
            />
          </div>

          {errore && (
            <div className="alert alert-danger py-2" role="alert" data-testid="carica-errore">
              {errore}
            </div>
          )}

          <div className="d-flex justify-content-end gap-2">
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={chiudi}
              disabled={inCorso}
              data-testid="carica-annulla"
            >
              Annulla
            </button>
            <button
              type="submit"
              className="btn btn-leggi"
              disabled={inCorso || !file}
              data-testid="carica-invia"
            >
              {inCorso ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" />
                  Sto leggendo il libro...
                </>
              ) : (
                'Carica'
              )}
            </button>
          </div>
        </form>
      )}
    </div>
  )
}