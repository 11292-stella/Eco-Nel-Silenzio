package it.econelsilenzio.libri.repository;

import it.econelsilenzio.libri.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    // I libri dal più recente al più vecchio (per l'elenco nella home)
    List<Libro> findAllByOrderByDataCaricamentoDesc();

    /**
     * Cancella un libro con UNA sola query.
     * Pagine e frasi le toglie PostgreSQL da solo (ON DELETE CASCADE nella migration),
     * senza caricarle in memoria come farebbe deleteById.
     *
     * @return quante righe ha cancellato: 1 se il libro c'era, 0 se non esisteva
     */
    @Modifying
    @Query("DELETE FROM Libro l WHERE l.id = :id")
    int eliminaPerId(Long id);
}
