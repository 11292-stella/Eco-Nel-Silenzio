import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

// Bootstrap: prima lo stile, poi il JavaScript (serve per menu a tendina, modali, chiusura degli avvisi...)
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'

// Caratteri antichi (installati con Fontsource, funzionano anche offline)
import '@fontsource/im-fell-english'   // titoli
import '@fontsource/eb-garamond'       // testo

import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)