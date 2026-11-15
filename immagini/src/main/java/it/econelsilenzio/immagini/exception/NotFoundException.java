package it.econelsilenzio.immagini.exception;

/**
 * Tema o immagine che non esiste: diventa una risposta 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}