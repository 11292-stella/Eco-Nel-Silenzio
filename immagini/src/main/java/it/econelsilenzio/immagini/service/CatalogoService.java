package it.econelsilenzio.immagini.service;

import it.econelsilenzio.immagini.config.ImmaginiProperties;
import it.econelsilenzio.immagini.dto.ImmagineDto;
import it.econelsilenzio.immagini.dto.TemaDto;
import it.econelsilenzio.immagini.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Il catalogo delle atmosfere: una cartella per tema, con dentro le immagini e un tema.json.
 *
 *   catalogo/
 *   └── notte/
 *       ├── tema.json               { "titolo": "Notte", "descrizione": ..., "idealePer": ..., "ordine": 1 }
 *       ├── 01-luna-sul-porto.webp
 *       └── 02-cielo-stellato.webp
 *
 * Niente database: si aggiunge un'immagine nella cartella e compare, senza ricompilare.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogoService {

    // Nomi ammessi: SOLO questi caratteri. Cosi' "..", "/" e "\" non possono mai entrare in un percorso
    private static final Pattern NOME_TEMA = Pattern.compile("[a-z0-9_-]+");
    private static final Pattern NOME_FILE = Pattern.compile("[A-Za-z0-9_-]+\\.(webp|webm|png|jpg|jpeg)");

    private static final String FILE_TEMA = "tema.json";
    private static final String URL_FILE = "/api/immagini/file/";
    private static final int ORDINE_IN_FONDO = 999;      // temi senza "ordine": dopo tutti gli altri

    private final ImmaginiProperties props;
    private final JsonMapper jsonMapper;                 // Jackson 3 (Spring Boot 4): legge i tema.json

    /** Il contenuto di tema.json. Ogni campo puo' mancare. */
    record InfoTema(String titolo, String descrizione, String idealePer, Integer ordine) {
        static final InfoTema VUOTA = new InfoTema(null, null, null, null);
    }

    // ================= Le tre operazioni del catalogo =================

    /**
     * Tutti i temi, in ordine (prima "ordine" del tema.json, poi il nome).
     * Se la cartella del catalogo non c'e', nessun tema (non un errore).
     */
    public List<TemaDto> temi() {
        Path base = cartellaCatalogo();
        if (!Files.isDirectory(base)) {
            log.warn("Cartella del catalogo non trovata: {}", base);
            return List.of();
        }
        try (Stream<Path> cartelle = Files.list(base)) {
            return cartelle
                    .filter(Files::isDirectory)
                    .filter(c -> NOME_TEMA.matcher(c.getFileName().toString()).matches())
                    .map(this::tema)
                    .sorted(Comparator.comparingInt(TemaDto::ordine).thenComparing(TemaDto::nome))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile leggere il catalogo", e);
        }
    }

    /**
     * Le immagini di un tema, in ordine di nome (01-, 02-...), ognuna con il suo indirizzo.
     *
     * @throws NotFoundException se il tema non esiste (→ 404)
     */
    public List<ImmagineDto> immagini(String tema) {
        Path cartella = cartellaTema(tema);
        return nomiImmagini(cartella).stream()
                .map(nome -> new ImmagineDto(nome, URL_FILE + tema + "/" + nome))
                .toList();
    }

    /**
     * Il file di un'immagine, controllato: deve stare DENTRO la cartella del tema.
     *
     * @throws NotFoundException se il tema o il file non esistono, o se il nome non e' ammesso (→ 404)
     */
    public Path file(String tema, String nome) {
        Path cartella = cartellaTema(tema);

        // 1. il nome deve avere la forma giusta (niente "..", niente "/")
        if (nome == null || !NOME_FILE.matcher(nome).matches()) {
            throw new NotFoundException("Immagine non trovata");
        }
        // 2. per sicurezza in piu': il percorso finale deve restare dentro la cartella del tema
        Path file = cartella.resolve(nome).normalize();
        if (!file.startsWith(cartella) || !Files.isRegularFile(file)) {
            throw new NotFoundException("Immagine non trovata");
        }
        return file;
    }

    // ================= Metodi di supporto =================

    // Un tema: nome della cartella + informazioni del tema.json + quante immagini ha
    private TemaDto tema(Path cartella) {
        String nome = cartella.getFileName().toString();
        InfoTema info = leggiInfo(cartella);

        return new TemaDto(
                nome,
                testoOppure(info.titolo(), maiuscola(nome)),     // senza titolo: "notte" → "Notte"
                testoOppure(info.descrizione(), ""),
                testoOppure(info.idealePer(), ""),
                info.ordine() != null ? info.ordine() : ORDINE_IN_FONDO,
                nomiImmagini(cartella).size()
        );
    }

    // Legge tema.json. Se manca o e' scritto male il tema funziona lo stesso (con i valori di riserva)
    InfoTema leggiInfo(Path cartella) {
        Path json = cartella.resolve(FILE_TEMA);
        if (!Files.isRegularFile(json)) {
            return InfoTema.VUOTA;
        }
        try {
            return jsonMapper.readValue(Files.readString(json, StandardCharsets.UTF_8), InfoTema.class);
        } catch (IOException | JacksonException e) {
            log.warn("{} non leggibile, uso i valori di riserva: {}", json, e.getMessage());
            return InfoTema.VUOTA;
        }
    }

    // I nomi delle immagini di una cartella, in ordine alfabetico (tema.json, Thumbs.db... esclusi)
    private List<String> nomiImmagini(Path cartella) {
        try (Stream<Path> file = Files.list(cartella)) {
            return file
                    .filter(Files::isRegularFile)
                    .map(f -> f.getFileName().toString())
                    .filter(nome -> NOME_FILE.matcher(nome).matches())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile leggere la cartella " + cartella, e);
        }
    }

    // La cartella di un tema, controllata come i file
    private Path cartellaTema(String tema) {
        if (tema == null || !NOME_TEMA.matcher(tema).matches()) {
            throw new NotFoundException("Tema non trovato");
        }
        Path base = cartellaCatalogo();
        Path cartella = base.resolve(tema).normalize();
        if (!cartella.startsWith(base) || !Files.isDirectory(cartella)) {
            throw new NotFoundException("Tema '" + tema + "' non trovato");
        }
        return cartella;
    }

    // Percorso assoluto e "pulito" del catalogo: serve per confrontarlo con startsWith
    private Path cartellaCatalogo() {
        return Path.of(props.catalogo().cartella()).toAbsolutePath().normalize();
    }

    private static String testoOppure(String testo, String riserva) {
        return testo != null && !testo.isBlank() ? testo.strip() : riserva;
    }

    private static String maiuscola(String testo) {
        return testo.isEmpty() ? testo : Character.toUpperCase(testo.charAt(0)) + testo.substring(1);
    }
}