package it.econelsilenzio.libri.service;

/*
 * Test di PreparazioneAudioService: senza Spring, senza database, senza RabbitMQ.
 * Repository e RabbitTemplate sono finti (Mockito).
 *
 * Avvio (dalla cartella libri):
 *   ./mvnw test -Dtest=PreparazioneAudioServiceTest     solo questa classe
 *   ./mvnw test                                         tutti i test di libri (serve Docker Desktop per Testcontainers)
 */

import it.econelsilenzio.libri.config.PreparazioneProperties;
import it.econelsilenzio.libri.messaggi.FraseDaPreparare;
import it.econelsilenzio.libri.model.Frase;
import it.econelsilenzio.libri.model.Pagina;
import it.econelsilenzio.libri.repository.PaginaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreparazioneAudioServiceTest {

    private static final String EXCHANGE = "eco.voce";
    private static final String ROUTING_KEY = "prepara";

    @Mock
    private PaginaRepository paginaRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("Aperta la pagina 3 → in coda le frasi della pagina 4, in ordine")
    void frasiDellaPaginaDopo() {
        when(paginaRepository.findByLibroIdAndNumero(5L, 4))
                .thenReturn(Optional.of(pagina(4, "Era buio.", "Pioveva sul porto.")));

        service(1).preparaDopo(5L, 3);

        ArgumentCaptor<Object> messaggi = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, times(2)).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), messaggi.capture());
        assertThat(messaggi.getAllValues()).containsExactly(
                new FraseDaPreparare(5L, 4, "Era buio."),
                new FraseDaPreparare(5L, 4, "Pioveva sul porto."));

        // con pagineAvanti = 1 la pagina 5 non si cerca nemmeno
        verify(paginaRepository, never()).findByLibroIdAndNumero(5L, 5);
    }

    @Test
    @DisplayName("Pagine avanti = 2 → in coda le frasi della pagina 4 e poi della 5")
    void duePagineAvanti() {
        when(paginaRepository.findByLibroIdAndNumero(5L, 4))
                .thenReturn(Optional.of(pagina(4, "Uno.", "Due.")));
        when(paginaRepository.findByLibroIdAndNumero(5L, 5))
                .thenReturn(Optional.of(pagina(5, "Tre.")));

        service(2).preparaDopo(5L, 3);

        ArgumentCaptor<Object> messaggi = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, times(3)).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), messaggi.capture());
        assertThat(messaggi.getAllValues()).containsExactly(
                new FraseDaPreparare(5L, 4, "Uno."),
                new FraseDaPreparare(5L, 4, "Due."),
                new FraseDaPreparare(5L, 5, "Tre."));
    }

    @Test
    @DisplayName("Ultima pagina del libro → niente in coda, nessun errore")
    void fineDelLibro() {
        when(paginaRepository.findByLibroIdAndNumero(5L, 13)).thenReturn(Optional.empty());

        assertThatCode(() -> service(1).preparaDopo(5L, 12)).doesNotThrowAnyException();

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("Il libro finisce prima delle pagine avanti → si ferma al primo buco")
    void siFermaAFineLibro() {
        when(paginaRepository.findByLibroIdAndNumero(5L, 12))
                .thenReturn(Optional.of(pagina(12, "Fine.")));
        when(paginaRepository.findByLibroIdAndNumero(5L, 13)).thenReturn(Optional.empty());

        service(3).preparaDopo(5L, 11);

        verify(rabbitTemplate, times(1)).convertAndSend(EXCHANGE, ROUTING_KEY, new FraseDaPreparare(5L, 12, "Fine."));
        // dopo la 13 che manca, la 14 non si cerca
        verify(paginaRepository, never()).findByLibroIdAndNumero(5L, 14);
    }

    @Test
    @DisplayName("RabbitMQ spento → nessuna eccezione e si smette al primo tentativo")
    void rabbitSpento() {
        when(paginaRepository.findByLibroIdAndNumero(5L, 4))
                .thenReturn(Optional.of(pagina(4, "Uno.", "Due.", "Tre.")));
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // la lettura non deve mai cadere per colpa della preparazione
        assertThatCode(() -> service(1).preparaDopo(5L, 3)).doesNotThrowAnyException();

        // un solo tentativo: non aspetta il timeout di connessione per ogni frase
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // --- utilità ---

    private PreparazioneAudioService service(int pagineAvanti) {
        return new PreparazioneAudioService(
                paginaRepository, rabbitTemplate,
                new PreparazioneProperties(EXCHANGE, ROUTING_KEY, pagineAvanti));
    }

    // Una pagina con le sue frasi numerate da 1, come la salverebbe LibroService
    private Pagina pagina(int numero, String... testi) {
        Pagina pagina = new Pagina();
        pagina.setNumero(numero);
        for (int i = 0; i < testi.length; i++) {
            Frase frase = new Frase();
            frase.setNumero(i + 1);
            frase.setTesto(testi[i]);
            pagina.aggiungiFrase(frase);
        }
        return pagina;
    }
}