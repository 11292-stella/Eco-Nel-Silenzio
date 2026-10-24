package it.econelsilenzio.libri.exception;

import it.econelsilenzio.libri.dto.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Trasforma le eccezioni lanciate dai controller in risposte HTTP
 * con un corpo ApiError, sempre nella stessa forma.
 */
@Slf4j
@RestControllerAdvice
public class CustomizeExceptionHandler {

    // Libro o pagina che non esiste → 404
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex) {
        return risposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // File vuoto, non PDF, PDF rotto → 400
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> validation(ValidationException ex) {
        return risposta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // File più grande del limite in application.properties (50MB) → 413
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> fileTroppoGrande(MaxUploadSizeExceededException ex) {
        return risposta(HttpStatus.PAYLOAD_TOO_LARGE, "Il file supera la dimensione massima consentita");
    }

    // Qualsiasi altro errore imprevisto → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> erroreGenerico(Exception ex) {
        log.error("Errore imprevisto", ex);
        return risposta(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno del server");
    }

    private ResponseEntity<ApiError> risposta(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(ApiError.di(status.value(), message));
    }
}