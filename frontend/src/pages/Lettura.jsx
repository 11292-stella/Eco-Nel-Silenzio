import { Link, useParams } from 'react-router'
import CorniceAntica from '../components/CorniceAntica'

/**
 * La pagina di lettura di un libro: /libri/:id
 * Per ora un segnaposto: mostra solo l'id letto dall'indirizzo.
 */
export default function Lettura() {
  const { id } = useParams()   // da "/libri/5" prende "5"

  return (
    <CorniceAntica>
      <h2 className="libreria-titolo">Lettura del libro {id}</h2>
      <p className="libreria-sottotitolo">Qui si aprirà il libro.</p>
      <p className="text-center">
        <Link to="/" className="btn btn-leggi">← Torna alla libreria</Link>
      </p>
    </CorniceAntica>
  )
}