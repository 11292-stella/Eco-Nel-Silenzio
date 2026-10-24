package it.econelsilenzio.libri.dto;

import java.time.LocalDateTime;

/**
 * I dati che mostriamo al client quando c'è un errore.
 */
public record ApiError(
        int status,
        String message,
        LocalDateTime dataErrore
) {

    public static ApiError di(int status, String message) {
        return new ApiError(status, message, LocalDateTime.now());
    }
}