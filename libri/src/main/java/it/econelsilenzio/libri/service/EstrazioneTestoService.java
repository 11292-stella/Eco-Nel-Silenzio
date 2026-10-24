package it.econelsilenzio.libri.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class EstrazioneTestoService {

    // Quante righe in cima e in fondo a ogni pagina controllo
    private static final int RIGHE_DA_CONTROLLARE = 2;

    /**
     * Apre il PDF e restituisce il testo di ogni pagina, già ripulito.
     * La posizione nella lista corrisponde alla pagina: indice 0 = pagina 1.
     */
    public List<String> estraiPagine(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();

            // 1. Testo "grezzo" di ogni pagina, ancora con gli a capo
            List<String> grezze = new ArrayList<>();
            for (int numero = 1; numero <= documento.getNumberOfPages(); numero++) {
                stripper.setStartPage(numero);
                stripper.setEndPage(numero);
                grezze.add(stripper.getText(documento));
            }

            // 2. Trovo intestazioni e piè di pagina ripetuti
            Set<String> ripetute = righeRipetute(grezze);

            // 3. Le tolgo e pulisco il testo
            List<String> pagine = new ArrayList<>();
            for (String testo : grezze) {
                pagine.add(pulisci(togliRighe(testo, ripetute)));
            }
            return pagine;
        }
    }

    /**
     * Righe in cima o in fondo che compaiono in almeno metà delle pagine.
     * Restituisce la loro forma "normalizzata".
     */
    Set<String> righeRipetute(List<String> pagine) {
        Set<String> ripetute = new HashSet<>();
        if (pagine.size() < 3) {
            return ripetute;               // troppe poche pagine per capirlo
        }

        Map<String, Integer> conteggio = new HashMap<>();
        for (String pagina : pagine) {
            // ogni riga conta una volta sola per pagina
            Set<String> viste = new HashSet<>();
            for (String riga : righeAiBordi(pagina)) {
                viste.add(normalizza(riga));
            }
            for (String chiave : viste) {
                conteggio.merge(chiave, 1, Integer::sum);
            }
        }

        int soglia = (pagine.size() + 1) / 2;   // almeno metà delle pagine
        conteggio.forEach((chiave, volte) -> {
            if (volte >= soglia) {
                ripetute.add(chiave);
            }
        });
        return ripetute;
    }

    /**
     * Toglie dalla pagina le righe ripetute, ma solo in cima e in fondo:
     * così una battuta come «Sì.» in mezzo al testo non viene mai toccata.
     */
    String togliRighe(String testo, Set<String> ripetute) {
        List<String> righe = new ArrayList<>(Arrays.asList(testo.split("\\R")));
        righe.removeIf(String::isBlank);

        for (int i = 0; i < RIGHE_DA_CONTROLLARE && !righe.isEmpty(); i++) {
            if (ripetute.contains(normalizza(righe.getFirst()))) {
                righe.removeFirst();
            }
            if (!righe.isEmpty() && ripetute.contains(normalizza(righe.getLast()))) {
                righe.removeLast();
            }
        }
        return String.join("\n", righe);
    }

    // Le prime e le ultime righe non vuote della pagina
    private List<String> righeAiBordi(String pagina) {
        List<String> righe = Arrays.stream(pagina.split("\\R"))
                .filter(r -> !r.isBlank())
                .toList();
        int n = Math.min(RIGHE_DA_CONTROLLARE, righe.size());
        List<String> bordi = new ArrayList<>(righe.subList(0, n));
        bordi.addAll(righe.subList(righe.size() - n, righe.size()));
        return bordi;
    }

    // "Capitolo 1 – pagina 2" e "Capitolo 1 – pagina 3" diventano uguali
    String normalizza(String riga) {
        return riga.trim().toLowerCase().replaceAll("\\d+", "#");
    }

    /**
     * Il testo estratto dai PDF arriva "sporco":
     * parole spezzate a fine riga ("ca-\nsa") e a capo in mezzo alle frasi.
     */
    String pulisci(String testo) {
        return testo
                .replaceAll("(\\p{L})-\\s*\\R\\s*(\\p{L})", "$1$2")
                .replaceAll("\\s+", " ")
                .trim();
    }
}