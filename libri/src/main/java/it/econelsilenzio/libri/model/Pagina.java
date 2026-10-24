package it.econelsilenzio.libri.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pagina")
@Getter
@Setter
@NoArgsConstructor
public class Pagina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String testo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false)
    private Libro libro;

    @OneToMany(mappedBy = "pagina", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<Frase> frasi = new ArrayList<>();

    // Aggiunge una frase e tiene allineati i due lati della relazione
    public void aggiungiFrase(Frase frase) {
        frasi.add(frase);
        frase.setPagina(this);
    }
}