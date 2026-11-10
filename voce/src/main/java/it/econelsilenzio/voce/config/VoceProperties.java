package it.econelsilenzio.voce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Le impostazioni del servizio voce, lette da application.properties (prefisso "voce").
 *
 * voce.piper.url=http://...         →  piper().url()
 * voce.piper.length-scale=1.1       →  piper().lengthScale()
 * voce.ffmpeg.tonalita=0.92         →  ffmpeg().tonalita()
 */
@ConfigurationProperties(prefix = "voce")
public record VoceProperties(Piper piper, Ffmpeg ffmpeg, Audio audio) {

    /** Piper: il server HTTP sempre acceso che trasforma il testo in voce. */
    public record Piper(
            String url,            // dove risponde il server di Piper (POST /synthesize)
            double lengthScale,    // velocita': 1.1 = un po' piu' lento
            double noiseScale      // regolarita': 0.5 = meno metallico
    ) {}

    /** ffmpeg: abbassa la tonalita' e converte in mp3. */
    public record Ffmpeg(
            String comando,        // "ffmpeg"
            double tonalita,       // 0.92 = voce un po' piu' bassa
            int frequenza          // 16000: la qualita' audio della voce di Riccardo
    ) {}

    /** Dove salvare gli mp3 generati. */
    public record Audio(String cartella) {}
}