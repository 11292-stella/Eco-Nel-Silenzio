package it.econelsilenzio.voce.exception;

import it.econelsilenzio.voce.service.SintesiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;

/**
 * Trasforma le eccezioni in risposte JSON, nella stessa forma del servizio libri:
 * { "status": ..., "message": ..., "dataErrore": ... }
 */
@Slf4j
@RestControllerAdvice
public class GestoreErrori {

    public record ApiError(int status, String message, LocalDateTime dataErrore) {}

    // Manca il parametro "testo" → 400
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> parametroMancante(MissingServletRequestParameterException ex) {
        return risposta(HttpStatus.BAD_REQUEST, "Manca il testo da leggere");
    }

    // Testo vuoto o oltre 1000 caratteri (@NotBlank, @Size) → 400
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> testoNonValido(HandlerMethodValidationException ex) {
        return risposta(HttpStatus.BAD_REQUEST, "Il testo deve avere tra 1 e 1000 caratteri");
    }

    // Piper o ffmpeg non funzionano → 503: il servizio c'e', ma la voce adesso non e' disponibile
    @ExceptionHandler(SintesiException.class)
    public ResponseEntity<ApiError> voceNonDisponibile(SintesiException ex) {
        log.error("Sintesi fallita: {}", ex.getMessage(), ex);
        return risposta(HttpStatus.SERVICE_UNAVAILABLE, "La voce narrante non è disponibile in questo momento");
    }

    // Qualsiasi altro errore → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> erroreGenerico(Exception ex) {
        log.error("Errore imprevisto", ex);
        return risposta(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno del server");
    }

    private ResponseEntity<ApiError> risposta(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message, LocalDateTime.now()));
    }
}