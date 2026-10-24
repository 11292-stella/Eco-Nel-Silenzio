package it.econelsilenzio.libri.controller;

import it.econelsilenzio.libri.dto.LibroDto;
import it.econelsilenzio.libri.dto.PaginaDto;
import it.econelsilenzio.libri.exception.ValidationException;
import it.econelsilenzio.libri.service.LibroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/libri")
@RequiredArgsConstructor
public class LibroController {

    private final LibroService libroService;

    /**
     * Carica un PDF: lo divide in pagine e frasi e lo salva.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LibroDto carica(@RequestParam("file") MultipartFile file,
                           @RequestParam(value = "titolo", required = false) String titolo) {
        controllaPdf(file);
        try {
            return LibroDto.da(
                    libroService.carica(titolo, file.getOriginalFilename(), file.getBytes()));
        } catch (IOException e) {
            throw new ValidationException("Impossibile leggere il file caricato");
        }
    }

    /**
     * Elenco dei libri, dal più recente.
     */
    @GetMapping
    public List<LibroDto> elenco() {
        return libroService.elenco().stream()
                .map(LibroDto::da)
                .toList();
    }

    /**
     * Una pagina del libro con le sue frasi.
     */
    @GetMapping("/{id}/pagine/{numero}")
    public PaginaDto pagina(@PathVariable Long id, @PathVariable int numero) {
        return libroService.pagina(id, numero);
    }

    // Controlli sul file prima di passarlo al service
    private void controllaPdf(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("Il file è vuoto");
        }
        String nome = file.getOriginalFilename();
        if (nome == null || !nome.toLowerCase().endsWith(".pdf")) {
            throw new ValidationException("Il file deve essere un PDF");
        }
    }
}