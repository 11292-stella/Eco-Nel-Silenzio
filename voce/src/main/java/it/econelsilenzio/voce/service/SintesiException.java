package it.econelsilenzio.voce.service;

/**
 * La voce non e' riuscita a generare l'audio (Piper o ffmpeg mancanti, bloccati o in errore).
 * E' un errore del server, non di chi chiama: diventera' un 503.
 */
public class SintesiException extends RuntimeException {

    public SintesiException(String message) {
        super(message);
    }

    public SintesiException(String message, Throwable cause) {
        super(message, cause);
    }
}
