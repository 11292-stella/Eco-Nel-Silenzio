package it.econelsilenzio.voce.controller;

import it.econelsilenzio.voce.service.SintesiService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Duration;

// Niente @Validated: @NotBlank e @Size li controlla Spring MVC da solo e lancia
// HandlerMethodValidationException, che il GestoreErrori trasforma in 400.
// Con @Validated arrivava ConstraintViolationException → 500 (bug trovato da VoceControllerTest)
@RestController
@RequestMapping("/api/voce")
@RequiredArgsConstructor
public class VoceController {

    private final SintesiService sintesiService;

    /**
     * La frase letta da Riccardo, come file mp3.
     * Esempio: GET /api/voce/sintesi?testo=C'era una volta un piccolo porto
     */
    @GetMapping("/sintesi")
    public ResponseEntity<Resource> sintesi(
            @RequestParam @NotBlank @Size(max = 1000) String testo) {

        Path mp3 = sintesiService.sintetizza(testo.strip());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                // la stessa frase da' sempre lo stesso audio: il browser puo' tenerlo per un giorno
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                .body(new FileSystemResource(mp3));
    }
}