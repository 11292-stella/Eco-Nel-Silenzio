package it.econelsilenzio.libri.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "libro")
@Getter
@Setter
@NoArgsConstructor
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titolo;

    @Column(nullable = false)
    private String nomeFile;

    @Column(nullable = false)
    private int numeroPagine;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCaricamento;

    @OneToMany(mappedBy = "libro", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<Pagina> pagine = new ArrayList<>();

    @PrePersist
    void prePersist() {
        this.dataCaricamento = LocalDateTime.now();
    }

    // Aggiunge una pagina e tiene allineati i due lati della relazione
    public void aggiungiPagina(Pagina pagina) {
        pagine.add(pagina);
        pagina.setLibro(this);
        this.numeroPagine = pagine.size();
    }
}
