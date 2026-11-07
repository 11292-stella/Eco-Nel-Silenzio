import { Route, Routes } from 'react-router'
import Navbar from './components/Navbar'
import Libreria from './components/Libreria'
import Lettura from './pages/Lettura'

function App() {
  return (
    <>
      {/* La navbar c'e' su tutte le pagine */}
      <Navbar />

      <main className="container py-4">
        {/* Cambia solo questa parte, in base all'indirizzo */}
        <Routes>
          <Route path="/" element={<Libreria />} />
          <Route path="/libri/:id" element={<Lettura />} />
        </Routes>
      </main>
    </>
  )
}

export default App
