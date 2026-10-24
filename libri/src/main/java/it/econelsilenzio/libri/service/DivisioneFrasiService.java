package it.econelsilenzio.libri.service;

import org.springframework.stereotype.Service;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DivisioneFrasiService {

    private static final Locale ITALIANO = Locale.ITALIAN;

    // Abbreviazioni dopo le quali la frase NON finisce (in minuscolo)
    private static final Set<String> ABBREVIAZIONI = Set.of(
            "sig.", "sigg.", "dott.", "prof.", "avv.", "ing.", "arch.",
            "dr.", "on.", "sen.", "mons.", "pag.", "cap.", "vol.", "fig.", "es."
    );

    /**
     * Divide il testo di una pagina in frasi, nell'ordine in cui compaiono.
     * Le frasi vuote vengono scartate.
     */
    public List<String> dividiInFrasi(String testo) {
        List<String> frasi = new ArrayList<>();
        if (testo == null || testo.isBlank()) {
            return frasi;
        }

        // 1. BreakIterator divide in "pezzi" (a volte troppo piccoli)
        BreakIterator iteratore = BreakIterator.getSentenceInstance(ITALIANO);
        iteratore.setText(testo);

        int inizio = iteratore.first();
        int fine = iteratore.next();

        while (fine != BreakIterator.DONE) {
            String pezzo = testo.substring(inizio, fine).trim();
            if (!pezzo.isEmpty()) {
                // 2. Se il pezzo precedente finiva con un'abbreviazione, lo riattacco
                if (!frasi.isEmpty() && finisceConAbbreviazione(frasi.getLast())) {
                    frasi.set(frasi.size() - 1, frasi.getLast() + " " + pezzo);
                } else {
                    frasi.add(pezzo);
                }
            }
            inizio = fine;
            fine = iteratore.next();
        }
        return frasi;
    }

    // "Il sig." → true    "G." → true (iniziale)    "Era buio." → false
    boolean finisceConAbbreviazione(String frase) {
        String ultimaParola = frase.substring(frase.lastIndexOf(' ') + 1);
        return ABBREVIAZIONI.contains(ultimaParola.toLowerCase())
                || ultimaParola.matches("\\p{Lu}\\.");   // iniziale: una maiuscola + punto
    }
}
