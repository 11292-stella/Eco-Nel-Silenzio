import logo from '../assets/logo.png'
import '../styles/Navbar.css'


export default function Navbar() {
  return (
    <nav className="navbar navbar-expand-lg navbar-eco" data-bs-theme="dark" data-testid="navbar">
      <div className="container">
        {/* Logo e nome */}
        <a className="navbar-brand d-flex align-items-center gap-2" href="/">
          <img src={logo} alt="Logo di Eco nel Silenzio" className="logo-eco" />
          Eco nel Silenzio
        </a>

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
              <a className="nav-link active" aria-current="page" href="#" data-testid="nav-libreria">
                Libreria
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="#" data-testid="nav-carica">
                Carica un libro
              </a>
            </li>
            <li className="nav-item">
              <span className="nav-link disabled" aria-disabled="true" data-testid="nav-atmosfere">
                Atmosfere <small className="badge-presto">presto</small>
              </span>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="#" data-testid="nav-progetto">
                Il progetto
              </a>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  )
}