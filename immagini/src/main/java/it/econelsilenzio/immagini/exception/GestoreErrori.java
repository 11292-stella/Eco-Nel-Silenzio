package it.econelsilenzio.immagini.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

/**
 * Trasforma le eccezioni in risposte con lo stesso formato di libri e voce.
 */
@Slf4j
@RestControllerAdvice
public class GestoreErrori {

    /** Il corpo delle risposte di errore: uguale negli altri servizi. */
    public record ApiError(int status, String message, LocalDateTime dataErrore) {}

    // Tema o immagine che non esiste (o nome non ammesso) → 404
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> nonTrovato(NotFoundException ex) {
        return risposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Indirizzo che non corrisponde a nessuna API (es. /api/immagini/file/notte senza il nome) → 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> indirizzoInesistente(NoResourceFoundException ex) {
        return risposta(HttpStatus.NOT_FOUND, "Indirizzo non trovato");
    }

    // Qualsiasi altro errore → 500, dettagli solo nel log
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> erroreGenerico(Exception ex) {
        log.error("Errore imprevisto", ex);
        return risposta(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno del server");
    }

    private ResponseEntity<ApiError> risposta(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), message, LocalDateTime.now()));
    }
}