package it.econelsilenzio.immagini.controller;

import it.econelsilenzio.immagini.dto.ImmagineDto;
import it.econelsilenzio.immagini.dto.TemaDto;
import it.econelsilenzio.immagini.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/immagini")
@RequiredArgsConstructor
public class ImmaginiController {

    private final CatalogoService catalogoService;

    /**
     * Tutti i temi, in ordine.
     * GET /api/immagini/temi
     */
    @GetMapping("/temi")
    public List<TemaDto> temi() {
        return catalogoService.temi();
    }

    /**
     * Le immagini di un tema, ognuna con il suo indirizzo. 404 se il tema non esiste.
     * GET /api/immagini/temi/notte
     */
    @GetMapping("/temi/{tema}")
    public List<ImmagineDto> immagini(@PathVariable String tema) {
        return catalogoService.immagini(tema);
    }

    /**
     * Il file di un'immagine. 404 se non esiste o se il nome non e' ammesso.
     * GET /api/immagini/file/notte/01-luna-sul-porto.webp
     */
    @GetMapping("/file/{tema}/{nome}")
    public ResponseEntity<Resource> file(@PathVariable String tema, @PathVariable String nome) {
        Path file = catalogoService.file(tema, nome);

        return ResponseEntity.ok()
                .contentType(tipo(nome))
                // un giorno di cache: se sostituisci un'immagine con lo stesso nome, il giorno dopo si vede la nuova
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                .body(new FileSystemResource(file));
    }

    // Il tipo del file dall'estensione (le estensioni ammesse sono solo queste: le controlla il service)
    static MediaType tipo(String nome) {
        String estensione = nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (estensione) {
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "webm" -> MediaType.parseMediaType("video/webm");
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}