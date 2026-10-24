package it.econelsilenzio.libri.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DivisioneFrasiServiceTest {

    private final DivisioneFrasiService service = new DivisioneFrasiService();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n"})
    @DisplayName("testo nullo, vuoto o solo spazi → nessuna frase")
    void testoVuoto(String testo) {
        assertThat(service.dividiInFrasi(testo)).isEmpty();
    }

    @Test
    @DisplayName("divide sui segni di fine frase: punto, esclamativo, interrogativo")
    void divideSuiSegniDiFineFrase() {
        assertThat(service.dividiInFrasi("Era una notte buia. Il cane abbaiò! Chi era?"))
                .containsExactly("Era una notte buia.", "Il cane abbaiò!", "Chi era?");
    }

    @Test
    @DisplayName("non divide sul punto dentro un numero")
    void nonDivideNeiNumeri() {
        assertThat(service.dividiInFrasi("Alle 3.30 partì il treno."))
                .containsExactly("Alle 3.30 partì il treno.");
    }

    @Test
    @DisplayName("ogni battuta di dialogo tra «» è una frase")
    void dialoghi() {
        assertThat(service.dividiInFrasi("«Più forte, ancora.» «Non fermarti.»"))
                .containsExactly("«Più forte, ancora.»", "«Non fermarti.»");
    }

    @Test
    @DisplayName("non divide dopo un'abbreviazione come sig.")
    void abbreviazioni() {
        assertThat(service.dividiInFrasi("Il sig. Rossi tornò a casa."))
                .containsExactly("Il sig. Rossi tornò a casa.");
    }

    @Test
    @DisplayName("non divide dopo l'iniziale di un nome")
    void iniziali() {
        assertThat(service.dividiInFrasi("Lo scrisse G. Verga. Era siciliano."))
                .containsExactly("Lo scrisse G. Verga.", "Era siciliano.");
    }
}