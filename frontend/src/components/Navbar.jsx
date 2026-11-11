import { Link, NavLink } from 'react-router'
import logo from '../assets/logo.png'
import '../styles/Navbar.css'

// NavLink aggiunge da solo "active" (e aria-current="page") quando l'indirizzo corrisponde
function classeLink({ isActive }) {
  return isActive ? 'nav-link active' : 'nav-link'
}

/**
 * Barra in alto: logo, nome del progetto e link alle sezioni.
 * Base: navbar responsive di Bootstrap 5.3, stile in styles/Navbar.css.
 * I link usano React Router: si cambia pagina senza ricaricare l'app.
 */
export default function Navbar() {
  return (
    <nav className="navbar navbar-expand-lg navbar-eco" data-testid="navbar">
      <div className="container">
        {/* Logo e nome: riportano alla libreria */}
        <Link className="navbar-brand d-flex align-items-center gap-2" to="/">
          <img src={logo} alt="Logo di Eco nel Silenzio" className="logo-eco" />
          Eco nel Silenzio
        </Link>

        {/* Pulsante "hamburger": compare solo sugli schermi piccoli */}
        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#menuPrincipale"
          aria-controls="menuPrincipale"
          aria-expanded="false"
          aria-label="Apri il menu"
        >
          <span className="navbar-toggler-icon" />
        </button>

        {/* I link: su schermi piccoli si aprono con il pulsante */}
        <div className="collapse navbar-collapse" id="menuPrincipale">
          <ul className="navbar-nav ms-auto">
            <li className="nav-item">
              {/* end: attivo solo su "/" esatto, non sulle pagine di lettura /libri/... */}
              <NavLink to="/" end className={classeLink} data-testid="nav-libreria">
                Libreria
              </NavLink>
            </li>
            <li className="nav-item">
              <span className="nav-link disabled" aria-disabled="true" data-testid="nav-atmosfere">
                Atmosfere <small className="badge-presto">presto</small>
              </span>
            </li>
            <li className="nav-item">
              <span className="nav-link disabled" aria-disabled="true" data-testid="nav-app">
                Scarica app <small className="badge-presto">presto</small>
              </span>
            </li>
            <li className="nav-item">
              <span className="nav-link disabled" aria-disabled="true" data-testid="nav-progetto">
                Il progetto <small className="badge-presto">presto</small>
              </span>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  )
}