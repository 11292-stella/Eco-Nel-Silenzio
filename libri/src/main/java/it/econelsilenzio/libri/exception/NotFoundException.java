package it.econelsilenzio.libri.exception;

/**
 * Risorsa non trovata (libro, pagina…): diventa una risposta 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}