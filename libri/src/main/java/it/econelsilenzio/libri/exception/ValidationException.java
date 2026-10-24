package it.econelsilenzio.libri.exception;

/**
 * Dati in ingresso non validi (file vuoto, file non PDF, PDF rotto…):
 * diventa una risposta 400.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}