import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

// Bootstrap: prima lo stile, poi il JavaScript (serve per menu a tendina, modali, chiusura degli avvisi...)
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'

import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)