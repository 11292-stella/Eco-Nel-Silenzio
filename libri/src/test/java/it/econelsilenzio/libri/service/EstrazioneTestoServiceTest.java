package it.econelsilenzio.libri.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstrazioneTestoServiceTest {

    // Unit test: niente Spring, niente database, creo il service a mano
    private final EstrazioneTestoService service = new EstrazioneTestoService();

    @Test
    @DisplayName("pulisci ricongiunge le parole spezzate a fine riga")
    void pulisciRicongiungeParoleSpezzate() {
        assertThat(service.pulisci("Tornò a ca-\nsa stanco."))
                .isEqualTo("Tornò a casa stanco.");
    }

    @Test
    @DisplayName("pulisci trasforma a capo e spazi multipli in un solo spazio")
    void pulisciToglieSpaziMultipli() {
        assertThat(service.pulisci("  Era una\nnotte    buia.  "))
                .isEqualTo("Era una notte buia.");
    }

    @Test
    @DisplayName("normalizza rende uguali righe che cambiano solo nei numeri")
    void normalizzaIgnoraINumeri() {
        assertThat(service.normalizza("Capitolo 1 – pagina 2"))
                .isEqualTo(service.normalizza("Capitolo 1 – pagina 3"));
    }

    @Test
    @DisplayName("righeRipetute trova l'intestazione presente su ogni pagina")
    void righeRipetuteTrovaIntestazione() {
        List<String> pagine = List.of(
                "Capitolo 1 – pagina 1\nPrima pagina del racconto.",
                "Capitolo 1 – pagina 2\nSeconda pagina del racconto.",
                "Capitolo 1 – pagina 3\nTerza pagina del racconto."
        );

        Set<String> ripetute = service.righeRipetute(pagine);

        assertThat(ripetute).contains("capitolo # – pagina #");
    }

    @Test
    @DisplayName("righeRipetute non decide niente con meno di 3 pagine")
    void righeRipetuteConPochePagine() {
        List<String> pagine = List.of("Intestazione\nTesto", "Intestazione\nAltro testo");

        assertThat(service.righeRipetute(pagine)).isEmpty();
    }

    @Test
    @DisplayName("togliRighe toglie l'intestazione ma non tocca il testo in mezzo")
    void togliRigheSoloAiBordi() {
        String pagina = """
                Capitolo 1 – pagina 2
                Disse lui, e se ne andò.
                «Sì.»
                Poi tornò il silenzio.
                """;
        // Facciamo finta che anche «Sì.» sia una riga ripetuta
        Set<String> ripetute = Set.of("capitolo # – pagina #", "«sì.»");

        String risultato = service.togliRighe(pagina, ripetute);

        assertThat(risultato)
                .doesNotContain("Capitolo")
                .contains("«Sì.»");                    // in mezzo: resta
    }

    // ===== Test con un PDF vero, creato al volo con PDFBox =====

    @Test
    @DisplayName("estraiPagine toglie intestazioni e numeri di pagina e ricongiunge le parole")
    void estraiPagineDaPdfVero() throws IOException {
        byte[] pdf = creaPdf(List.of(
                List.of("Racconto di prova - pagina 1",
                        "Prima pagina del racconto. Tornò a ca-",
                        "sa stanco.",
                        "1"),
                List.of("Racconto di prova - pagina 2",
                        "Seconda pagina.",
                        "Era una notte buia.",
                        "2"),
                List.of("Racconto di prova - pagina 3",
                        "Terza pagina.",
                        "Fine.",
                        "3")
        ));

        List<String> pagine = service.estraiPagine(pdf);

        assertThat(pagine).containsExactly(
                "Prima pagina del racconto. Tornò a casa stanco.",
                "Seconda pagina. Era una notte buia.",
                "Terza pagina. Fine."
        );
    }

    @Test
    @DisplayName("estraiPagine: una pagina senza testo diventa una stringa vuota")
    void paginaSenzaTesto() throws IOException {
        byte[] pdf = creaPdf(List.of(List.of()));   // una pagina, nessuna riga

        assertThat(service.estraiPagine(pdf)).containsExactly("");
    }

    @Test
    @DisplayName("estraiPagine: un file che non è un PDF lancia IOException")
    void fileNonPdf() {
        byte[] nonPdf = "non sono un pdf".getBytes();

        assertThatThrownBy(() -> service.estraiPagine(nonPdf))
                .isInstanceOf(IOException.class);
    }

    // ===== Metodi di supporto =====

    // Crea un PDF in memoria: ogni elemento della lista è una pagina,
    // ogni stringa è una riga di testo (dall'alto verso il basso)
    private byte[] creaPdf(List<List<String>> pagine) throws IOException {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream uscita = new ByteArrayOutputStream()) {

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            for (List<String> righe : pagine) {
                PDPage pagina = new PDPage();
                documento.addPage(pagina);

                try (PDPageContentStream contenuto = new PDPageContentStream(documento, pagina)) {
                    contenuto.beginText();
                    contenuto.setFont(font, 12);
                    contenuto.newLineAtOffset(50, 750);       // parto in alto a sinistra
                    for (String riga : righe) {
                        contenuto.showText(riga);
                        contenuto.newLineAtOffset(0, -20);    // riga successiva, 20 punti più in basso
                    }
                    contenuto.endText();
                }
            }

            documento.save(uscita);
            return uscita.toByteArray();
        }
    }
}