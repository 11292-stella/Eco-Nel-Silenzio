import { createPortal } from 'react-dom'
import '../styles/ConfermaElimina.css'

/**
 * Finestra di conferma prima di eliminare un libro.
 * Markup del "modal" di Bootstrap 5.3, ma lo mostra React:
 * se `libro` e' null la finestra non c'e'.
 *
 * Usa un PORTALE: l'HTML finisce direttamente nel <body>, fuori dalla cornice.
 * Serve perche' la cornice ha backdrop-filter, che "intrappola" gli elementi position: fixed.
 *
 * @param libro       il libro da eliminare, oppure null
 * @param inCorso     true mentre la cancellazione e' in corso (pulsanti bloccati)
 * @param errore      messaggio di errore da mostrare, oppure null
 * @param onConferma  chiamata quando si preme "Elimina"
 * @param onAnnulla   chiamata quando si preme "Annulla" o la X
 */
export default function ConfermaElimina({ libro, inCorso, errore, onConferma, onAnnulla }) {
  if (!libro) return null

  const titolo = libro.titolo.replaceAll('_', ' ')

  return createPortal(
    <>
      <div
        className="modal d-block modal-pergamena"
        tabIndex="-1"
        role="dialog"
        aria-modal="true"
        aria-labelledby="titoloConferma"
        data-testid="conferma-elimina"
      >
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header">
              <h5 className="modal-title" id="titoloConferma">Eliminare il libro?</h5>
              <button
                type="button"
                className="btn-close"
                aria-label="Chiudi"
                onClick={onAnnulla}
                disabled={inCorso}
              />
            </div>

            <div className="modal-body">
              <p className="mb-0">
                Vuoi davvero togliere <strong>«{titolo}»</strong> dallo scaffale?
                Verranno cancellate anche tutte le sue pagine. L'operazione non si può annullare.
              </p>

              {errore && (
                <div className="alert alert-danger mt-3 mb-0" role="alert" data-testid="conferma-elimina-errore">
                  {errore}
                </div>
              )}
            </div>

            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-annulla-antico"
                onClick={onAnnulla}
                disabled={inCorso}
                data-testid="conferma-elimina-annulla"
              >
                Annulla
              </button>
              <button
                type="button"
                className="btn btn-elimina-antico"
                onClick={onConferma}
                disabled={inCorso}
                data-testid="conferma-elimina-ok"
              >
                {inCorso ? 'Eliminazione...' : 'Elimina'}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* lo sfondo scuro dietro la finestra */}
      <div className="modal-backdrop fade show" />
    </>,
    document.body
  )
}