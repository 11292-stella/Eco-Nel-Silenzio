import Navbar from './components/Navbar'
import Libreria from './components/Libreria'

function App() {
  return (
    <>
      <Navbar />

      <main className="container py-4">
        <Libreria />
      </main>
    </>
  )
}

export default App
