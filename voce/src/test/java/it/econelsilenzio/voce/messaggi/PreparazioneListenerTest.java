package it.econelsilenzio.voce.messaggi;

/*
 * Test di PreparazioneListener: senza Spring e senza RabbitMQ.
 * Il metodo prepara(...) si chiama direttamente, SintesiService e' finto (Mockito).
 *
 * Avvio (dalla cartella voce):
 *   ./mvnw test -Dtest=PreparazioneListenerTest     solo questa classe
 *   ./mvnw test                                     tutti i test di voce
 */

import it.econelsilenzio.voce.service.SintesiException;
import it.econelsilenzio.voce.service.SintesiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreparazioneListenerTest {

    @Mock
    private SintesiService sintesiService;

    @InjectMocks
    private PreparazioneListener listener;     // creato da Mockito con il service finto

    @Test
    @DisplayName("Una frase dalla coda → SintesiService prepara il suo audio")
    void preparaLaFrase() {
        listener.prepara(new FraseDaPreparare(5L, 4, "Era una notte buia."));

        verify(sintesiService).sintetizza("Era una notte buia.");
    }

    @Test
    @DisplayName("Spazi all'inizio e alla fine → tolti, come nel controller (stesso nome in cache)")
    void testoSenzaSpazi() {
        listener.prepara(new FraseDaPreparare(5L, 4, "   Era buio.  "));

        verify(sintesiService).sintetizza("Era buio.");
    }

    @ParameterizedTest(name = "testo [{0}] → scartato")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("Testo mancante, vuoto o di soli spazi → scartato, Piper non viene chiamato")
    void testoVuotoScartato(String testo) {
        listener.prepara(new FraseDaPreparare(5L, 4, testo));

        verifyNoInteractions(sintesiService);
    }

    @Test
    @DisplayName("Testo oltre 1000 caratteri → scartato")
    void testoTroppoLungo() {
        listener.prepara(new FraseDaPreparare(5L, 4, "a".repeat(PreparazioneListener.MAX_CARATTERI + 1)));

        verifyNoInteractions(sintesiService);
    }

    @Test
    @DisplayName("Testo di esattamente 1000 caratteri → preparato (il limite e' compreso)")
    void testoAlLimite() {
        String testo = "a".repeat(PreparazioneListener.MAX_CARATTERI);

        listener.prepara(new FraseDaPreparare(5L, 4, testo));

        verify(sintesiService).sintetizza(testo);
    }

    @Test
    @DisplayName("Piper spento → nessuna eccezione: il messaggio viene confermato e non torna in coda")
    void piperSpento() {
        when(sintesiService.sintetizza(anyString()))
                .thenThrow(new SintesiException("Il server di Piper non e' disponibile"));

        // se l'eccezione uscisse dal metodo, Spring non manderebbe l'ack
        assertThatCode(() -> listener.prepara(new FraseDaPreparare(5L, 4, "Era buio.")))
                .doesNotThrowAnyException();

        verify(sintesiService).sintetizza("Era buio.");
    }
}