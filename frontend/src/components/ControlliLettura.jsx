import SigilloVoce from './SigilloVoce'

/**
 * I controlli della lettura, sopra il libro.
 * Per ora: il sigillo di ceralacca (ascolta / pausa).
 * Qui arriveranno anche frase precedente / successiva e la velocita'.
 *
 * @param voce       quello che restituisce useVoceNarrante: { inAscolto, alterna, ... }
 * @param disabled   true finche' la pagina non e' arrivata
 */
export default function ControlliLettura({ voce, disabled }) {
  return (
    <div className="lettura-intestazione" data-testid="lettura-controlli">
      {/* Il sigillo di ceralacca: ascolta / pausa */}
      <SigilloVoce
        inAscolto={voce.inAscolto}
        disabled={disabled}
        onClick={voce.alterna}
      />
    </div>
  )
}