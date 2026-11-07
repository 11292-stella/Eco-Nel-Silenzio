import '../styles/CorniceAntica.css'

/**
 * Riquadro con la cornice di legno antico.
 * Incornicia qualsiasi contenuto: <CorniceAntica> ... </CorniceAntica>
 */
export default function CorniceAntica({ children }) {
  return (
    <section className="cornice-antica" data-testid="cornice">
      {children}
    </section>
  )
}