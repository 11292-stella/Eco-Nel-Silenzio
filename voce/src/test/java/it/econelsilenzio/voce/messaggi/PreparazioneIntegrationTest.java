package it.econelsilenzio.voce.messaggi;

/*
 * Test di integrazione: Spring vero + RabbitMQ vero (in Docker, con Testcontainers).
 * Solo SintesiService e' finto: niente Piper e niente ffmpeg.
 * Controlla che coda, exchange e binding di RabbitConfig funzionino davvero
 * e che un messaggio scritto COME LO MANDA LIBRI arrivi al listener.
 *
 * Serve Docker Desktop avviato.
 * Avvio (dalla cartella voce):
 *   ./mvnw test -Dtest=PreparazioneIntegrationTest     solo questa classe
 *   ./mvnw test                                        tutti i test di voce
 */

import it.econelsilenzio.voce.TestcontainersConfiguration;
import it.econelsilenzio.voce.config.PreparazioneProperties;
import it.econelsilenzio.voce.service.SintesiException;
import it.econelsilenzio.voce.service.SintesiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)    // il RabbitMQ usa e getta
class PreparazioneIntegrationTest {

    // Il nome della classe di libri, come lo scrive il convertitore di libri nell'intestazione
    private static final String TYPE_ID_DI_LIBRI = "it.econelsilenzio.libri.messaggi.FraseDaPreparare";

    @Autowired
    private RabbitTemplate rabbitTemplate;     // per pubblicare "facendo finta di essere libri"

    @Autowired
    private AmqpAdmin amqpAdmin;               // per contare i messaggi rimasti in coda

    @Autowired
    private PreparazioneProperties props;      // eco.voce, prepara, voce.preparazione

    @MockitoBean
    private SintesiService sintesiService;     // finto: niente Piper

    @Test
    @DisplayName("Messaggio come lo manda libri (JSON + __TypeId__ di libri) → il listener prepara la frase")
    void messaggioDiLibri() {
        pubblica("""
                {"libroId":5,"pagina":4,"testo":"Era una notte buia."}""");

        // il listener lavora in un altro thread: si aspetta fino a 5 secondi
        verify(sintesiService, timeout(5000)).sintetizza("Era una notte buia.");
    }

    @Test
    @DisplayName("Piper spento → una sola chiamata, il messaggio NON torna in coda (niente giro infinito)")
    void piperSpentoNessunGiroInfinito() {
        when(sintesiService.sintetizza(anyString()))
                .thenThrow(new SintesiException("Il server di Piper non e' disponibile"));

        pubblica("""
                {"libroId":5,"pagina":4,"testo":"Uno."}""");

        // after: aspetta 2 secondi INTERI e poi controlla che le chiamate siano state esattamente 1
        verify(sintesiService, after(2000).times(1)).sintetizza("Uno.");
        assertThat(messaggiInCoda()).isZero();
    }

    @Test
    @DisplayName("Messaggio rotto (non JSON) → scartato senza chiamare Piper e senza bloccare la coda")
    void messaggioRotto() {
        pubblica("questo non e' JSON");

        verify(sintesiService, after(2000).never()).sintetizza(anyString());
        assertThat(messaggiInCoda()).isZero();

        // e la coda continua a funzionare per i messaggi dopo
        pubblica("""
                {"libroId":5,"pagina":4,"testo":"Dopo."}""");
        verify(sintesiService, timeout(5000)).sintetizza("Dopo.");
    }

    // --- utilità ---

    // Pubblica sull'exchange di libri un messaggio "a mano", con le stesse intestazioni di libri
    private void pubblica(String json) {
        MessageProperties proprieta = new MessageProperties();
        proprieta.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        proprieta.setContentEncoding("UTF-8");
        proprieta.setHeader("__TypeId__", TYPE_ID_DI_LIBRI);   // una classe che in voce NON esiste

        Message messaggio = new Message(json.getBytes(StandardCharsets.UTF_8), proprieta);
        rabbitTemplate.send(props.exchange(), props.routingKey(), messaggio);
    }

    private long messaggiInCoda() {
        return amqpAdmin.getQueueInfo(props.coda()).getMessageCount();
    }
}