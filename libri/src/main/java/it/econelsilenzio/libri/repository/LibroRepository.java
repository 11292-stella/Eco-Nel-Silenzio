package it.econelsilenzio.libri.repository;

import it.econelsilenzio.libri.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    // I libri dal più recente al più vecchio (per l'elenco nella home)
    List<Libro> findAllByOrderByDataCaricamentoDesc();
}
