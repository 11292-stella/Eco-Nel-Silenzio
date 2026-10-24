package it.econelsilenzio.libri.repository;

import it.econelsilenzio.libri.model.Pagina;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaginaRepository extends JpaRepository<Pagina, Long> {

    // Una pagina precisa di un libro, già con il libro e le frasi caricati
    @EntityGraph(attributePaths = {"libro", "frasi"})
    Optional<Pagina> findByLibroIdAndNumero(Long libroId, int numero);
}