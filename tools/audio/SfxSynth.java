import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Synthesises the game's sound effects and ten loopable ambient music tracks as 16-bit mono WAV files.
 *
 * <p>Everything is generated from code (sine/triangle oscillators, bell partials, filtered
 * noise), so the audio is original and free of third-party licences. Deterministic: the same
 * run always produces the same files. Effects are deliberately soft: fundamentals at or below
 * 1 kHz, no metallic partials, attacks of at least 8 ms, 2.8 kHz low-pass, peaks at -8 dBFS
 * (see {@link #soften}).</p>
 *
 * <pre>java tools\audio\SfxSynth.java games\sortpuzzle\android\src\main\assets\sfx</pre>
 */
public final class SfxSynth {

    private static final int SR = 44_100;
    private static final int MUSIC_SR = 22_050;

    private SfxSynth() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "sfx");
        Files.createDirectories(out);
        write(out, "pick", pick(), SR);
        write(out, "drop", drop(), SR);
        write(out, "invalid", invalid(), SR);
        write(out, "button", button(), SR);
        write(out, "coin", coin(), SR);
        write(out, "tube", tube(), SR);
        write(out, "hint", hint(), SR);
        write(out, "start", start(), SR);
        write(out, "win", win(), SR);
        for (int track = 0; track < 10; track++) {
            write(out, String.format(java.util.Locale.ROOT, "music_%02d", track + 1),
                    music(track), MUSIC_SR);
        }
    }

    // ------------------------------------------------------------------------------- sounds

    /** Ball lifted: a short rising "bloop", rounded (no click transient). */
    static double[] pick() {
        double[] b = new double[samples(0.16, SR)];
        sweep(b, SR, 0, 0.13, 340, 640, 0.8, 0.02, 0.05, Wave.SINE_OCTAVE);
        return b;
    }

    /** Ball lands: soft woody "tok" (D5 sine) with a knock underneath. Pitched up per slot in code. */
    static double[] drop() {
        double[] b = new double[samples(0.4, SR)];
        sweep(b, SR, 0, 0.3, 587.33, 587.33, 0.7, 0.012, 0.11, Wave.SINE_OCTAVE);
        sweep(b, SR, 0, 0.06, 220, 130, 0.4, 0.01, 0.02, Wave.TRIANGLE);
        return b;
    }

    /** Illegal move: low, soft double "bonk" - clearly "no" without being harsh. */
    static double[] invalid() {
        double[] b = new double[samples(0.3, SR)];
        sweep(b, SR, 0, 0.24, 196, 160, 0.55, 0.008, 0.09, Wave.TRIANGLE);
        sweep(b, SR, 0, 0.24, 207, 168, 0.45, 0.008, 0.09, Wave.TRIANGLE);
        noise(b, SR, 0, 0.05, 0.2, 0.004, 0.08, 11);
        return b;
    }

    /** UI tap: a soft, rounded "tup" around 500 Hz - felt more than heard. */
    static double[] button() {
        double[] b = new double[samples(0.08, SR)];
        sweep(b, SR, 0, 0.06, 520, 430, 0.7, 0.008, 0.016, Wave.SINE);
        return b;
    }

    /** Coins: two mellow plucked notes (E5 then B5), no metallic partials. */
    static double[] coin() {
        double[] b = new double[samples(0.7, SR)];
        sweep(b, SR, 0.000, 0.35, 659.25, 659.25, 0.55, 0.012, 0.10, Wave.SINE_OCTAVE);
        sweep(b, SR, 0.080, 0.55, 987.77, 987.77, 0.7, 0.012, 0.28, Wave.SINE_OCTAVE);
        return b;
    }

    /** A tube finished: warm rising arpeggio (G4-B4-D5-G5), plucked sines, no sparkle. */
    static double[] tube() {
        double[] b = new double[samples(1.3, SR)];
        double[] notes = {392.0, 493.88, 587.33, 783.99};
        for (int i = 0; i < notes.length; i++) {
            sweep(b, SR, i * 0.07, 0.9, notes[i], notes[i], 0.7 - i * 0.08, 0.015, 0.32, Wave.SINE_OCTAVE);
        }
        return b;
    }

    /** Hint shown: gentle shimmering arpeggio, one octave below the old one. */
    static double[] hint() {
        double[] b = new double[samples(1.0, SR)];
        double[] notes = {392.0, 493.88, 587.33, 783.99};
        for (int i = 0; i < notes.length; i++) {
            sweep(b, SR, i * 0.07, 0.6, notes[i], notes[i], 0.4, 0.02, 0.22, Wave.SINE_VIBRATO);
        }
        return b;
    }

    /** Level start: airy whoosh rising into the board. */
    static double[] start() {
        double[] b = new double[samples(0.5, SR)];
        Random r = new Random(5);
        double low = 0;
        int n = samples(0.42, SR);
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double cutoff = 0.015 + 0.09 * t;           // filter opens as the whoosh rises, but stays dark
            low += cutoff * ((r.nextDouble() * 2 - 1) - low);
            b[i] += low * Math.sin(Math.PI * t) * 0.9;
        }
        sweep(b, SR, 0.05, 0.4, 300, 900, 0.15, 0.1, 0.2, Wave.SINE);
        return b;
    }

    /** Level complete: plucked arpeggio up to a warm major chord (C4-C5 range), no sparkles. */
    static double[] win() {
        double[] b = new double[samples(2.0, SR)];
        double[] arp = {392.0, 493.88, 587.33, 783.99, 987.77};
        for (int i = 0; i < arp.length; i++) {
            sweep(b, SR, i * 0.09, 0.3, arp[i], arp[i], 0.6, 0.01, 0.12, Wave.PLUCK);
        }
        double[] chord = {261.63, 329.63, 392.0, 523.25};
        for (double f : chord) {
            bell(b, SR, 0.45, f, 0.5, 0.9);
        }
        return b;
    }

    /** A slow, seamless ambient loop with its own harmony, pacing and melody pattern. */
    static double[] music(int variant) {
        int sr = MUSIC_SR;
        int[] tempos = {64, 72, 68, 76, 62, 70, 78, 66, 74, 60};
        int[] keyOffsets = {0, 2, -2, 4, -4, 1, -1, 3, -3, 5};
        int[] melodyDensity = {22, 36, 16, 29, 12, 32, 24, 18, 40, 10};
        int tempo = tempos[variant];
        int keyOffset = keyOffsets[variant];
        double beat = 60.0 / tempo;
        double bar = beat * 4;
        double[] b = new double[samples(bar * 8, sr)];
        int[][] roots = {
            {0, 9, 5, 7, 0, 9, 2, 7},
            {0, 5, 2, 7, 9, 5, 0, 7},
            {0, 7, 3, 10, 5, 0, 7, 3},
            {0, 4, 9, 5, 0, 7, 2, 5},
            {0, 3, 8, 5, 10, 3, 0, 8},
            {0, 7, 5, 2, 9, 7, 0, 5},
            {0, 5, 8, 3, 10, 5, 0, 7},
            {0, 9, 7, 2, 5, 9, 0, 7},
            {0, 3, 10, 5, 8, 3, 0, 10},
            {0, 7, 2, 9, 5, 0, 7, 9}
        };
        boolean[][] major = {
            {true, false, true, true, true, false, false, true},
            {true, true, false, true, false, true, true, true},
            {true, false, true, true, true, false, true, false},
            {true, false, false, true, true, true, false, true},
            {false, true, true, false, true, true, false, true},
            {true, true, false, true, false, true, true, false},
            {true, false, true, true, false, true, true, true},
            {true, false, true, false, true, false, true, true},
            {false, true, false, true, true, false, true, false},
            {true, true, false, false, true, true, true, false}
        };
        int[][] melody = {
            {12, 14, 16, 19, 21, 24}, {12, 14, 17, 19, 21, 24},
            {12, 15, 17, 19, 22, 24}, {12, 14, 16, 19, 21, 23},
            {12, 15, 17, 20, 22, 24}, {12, 14, 17, 19, 21, 24},
            {12, 14, 16, 19, 21, 24}, {12, 15, 17, 19, 22, 24},
            {12, 14, 16, 18, 21, 24}, {12, 15, 17, 19, 22, 24}
        };
        Random r = new Random(96L + variant * 17L);
        for (int c = 0; c < 8; c++) {
            double t0 = c * bar;
            int root = 48 + keyOffset + roots[variant][c];
            int third = root + (major[variant][c] ? 4 : 3);
            int fifth = root + 7;
            for (int note : new int[] {root + 12, third + 12, fifth + 12}) {
                padWrapped(b, sr, t0 - 0.2, bar + 0.6, midi(note),
                        variant % 3 == 0 ? 0.09 : 0.07);
            }
            double bass = midi(root - 12);
            pluckWrapped(b, sr, t0, bass, 0.16, 1.15, Wave.SINE);
            if (variant % 3 != 2) {
                pluckWrapped(b, sr, t0 + (variant % 2 == 0 ? 2 : 3) * beat,
                    bass, 0.11, 0.9, Wave.SINE);
            }
            for (int eighth = 0; eighth < 8; eighth++) {
                int threshold = (eighth % 2 == 0 ? melodyDensity[variant]
                    : melodyDensity[variant] / 3);
                if (r.nextInt(100) < threshold) {
                    int note = melody[variant][r.nextInt(melody[variant].length)];
                    pluckWrapped(b, sr, t0 + eighth * beat / 2,
                        midi(root + note), variant % 2 == 0 ? 0.065 : 0.08,
                        variant % 2 == 0 ? 0.75 : 0.5,
                        variant % 2 == 0 ? Wave.SINE_OCTAVE : Wave.PLUCK);
                }
            }
        }
        normalize(b, 0.7);
        return b;
    }

    // ------------------------------------------------------------------------ synthesis kit

    enum Wave { SINE, SINE_OCTAVE, SINE_VIBRATO, TRIANGLE, PLUCK }

    static double osc(Wave w, double phase, double t) {
        switch (w) {
            case SINE_OCTAVE:
                return Math.sin(phase) + 0.25 * Math.sin(2 * phase);
            case SINE_VIBRATO:
                return Math.sin(phase + 0.15 * Math.sin(2 * Math.PI * 5.5 * t));
            case TRIANGLE:
                return 2 / Math.PI * Math.asin(Math.sin(phase));
            case PLUCK:
                return Math.sin(phase) + 0.35 * Math.sin(2 * phase) * Math.exp(-t * 18)
                        + 0.15 * Math.sin(3 * phase) * Math.exp(-t * 30);
            default:
                return Math.sin(phase);
        }
    }

    /** Oscillator with an exponential frequency glide and attack/exponential-decay envelope. */
    static void sweep(double[] b, int sr, double start, double dur, double f0, double f1,
                      double amp, double attack, double decay, Wave w) {
        int s0 = samples(start, sr);
        int n = samples(dur, sr);
        double phase = 0;
        for (int i = 0; i < n && s0 + i < b.length; i++) {
            double t = (double) i / sr;
            double f = f0 * Math.pow(f1 / f0, t / dur);
            phase += 2 * Math.PI * f / sr;
            double env = Math.min(1, t / attack) * Math.exp(-t / decay) * fadeTail(i, n);
            b[s0 + i] += amp * env * osc(w, phase, t);
        }
    }

    /** Struck glass/metal: inharmonic partials, higher ones dying faster. */
    static void bell(double[] b, int sr, double start, double f, double amp, double decay) {
        double[] ratio = {1.0, 2.0, 2.76, 5.4};
        double[] level = {1.0, 0.18, 0.05, 0.0};   // upper partials nearly off: they are what pierces
        double[] life = {1.0, 0.7, 0.45, 0.25};
        for (int p = 0; p < ratio.length; p++) {
            if (f * ratio[p] > sr / 2.2) {
                continue; // stay below Nyquist
            }
            sweep(b, sr, start, decay * 5, f * ratio[p], f * ratio[p], amp * level[p], 0.006,
                    decay * life[p], Wave.SINE);
        }
    }

    /** Random high pings, like glitter. */
    static void sparkle(double[] b, int sr, double from, double to, int count, double amp, long seed) {
        Random r = new Random(seed);
        for (int i = 0; i < count; i++) {
            double t = from + r.nextDouble() * (to - from);
            double f = 1600 + r.nextDouble() * 1800;
            sweep(b, sr, t, 0.25, f, f, amp * (0.4 + 0.6 * r.nextDouble()), 0.005, 0.05, Wave.SINE);
        }
    }

    /** Low-passed noise burst (clicks, thumps). */
    static void noise(double[] b, int sr, double start, double dur, double amp, double attack,
                      double cutoff, long seed) {
        Random r = new Random(seed);
        int s0 = samples(start, sr);
        int n = samples(dur, sr);
        double low = 0;
        for (int i = 0; i < n && s0 + i < b.length; i++) {
            double t = (double) i / sr;
            low += cutoff * ((r.nextDouble() * 2 - 1) - low);
            b[s0 + i] += amp * low * Math.min(1, t / attack) * (1 - (double) i / n);
        }
    }

    /** Slow-attack detuned pad; samples past the end wrap round to the start. */
    static void padWrapped(double[] b, int sr, double start, double dur, double f, double amp) {
        int s0 = samples(start, sr);
        int n = samples(dur, sr);
        double p1 = 0;
        double p2 = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            p1 += 2 * Math.PI * f * 1.003 / sr;
            p2 += 2 * Math.PI * f * 0.997 / sr;
            double env = Math.pow(Math.sin(Math.PI * t), 1.5);
            b[Math.floorMod(s0 + i, b.length)] += amp * env * (Math.sin(p1) + Math.sin(p2)) * 0.5;
        }
    }

    static void pluckWrapped(double[] b, int sr, double start, double f, double amp, double decay, Wave w) {
        int s0 = samples(start, sr);
        int n = samples(decay * 5, sr);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / sr;
            phase += 2 * Math.PI * f / sr;
            double env = Math.min(1, t / 0.004) * Math.exp(-t / decay) * fadeTail(i, n);
            b[Math.floorMod(s0 + i, b.length)] += amp * env * osc(w, phase, t);
        }
    }

    static double fadeTail(int i, int n) {
        int fade = Math.min(n / 4, 220);
        return i > n - fade ? (double) (n - i) / fade : 1;
    }

    static double midi(int note) {
        return 440.0 * Math.pow(2, (note - 69) / 12.0);
    }

    static int samples(double seconds, int sr) {
        return (int) Math.round(seconds * sr);
    }

    /**
     * Makes an effect easy on the ear: a 12 ms fade-in removes click transients, then two
     * cascaded one-pole low-pass filters (-6 dB at {@code cutoffHz}, -12 dB/octave above)
     * tame the 3-8 kHz band where hearing is most sensitive.
     */
    static void soften(double[] b, int sr, double cutoffHz) {
        int fadeIn = samples(0.012, sr);
        for (int i = 0; i < fadeIn && i < b.length; i++) {
            b[i] *= (double) i / fadeIn;
        }
        double a = 1 - Math.exp(-2 * Math.PI * cutoffHz / sr);
        double y1 = 0;
        double y2 = 0;
        for (int i = 0; i < b.length; i++) {
            y1 += a * (b[i] - y1);
            y2 += a * (y1 - y2);
            b[i] = y2;
        }
    }

    static void normalize(double[] b, double peak) {
        double max = 1e-9;
        for (double v : b) {
            max = Math.max(max, Math.abs(v));
        }
        for (int i = 0; i < b.length; i++) {
            b[i] = b[i] / max * peak;
        }
    }

    // ----------------------------------------------------------------------------- output

    static void write(Path dir, String name, double[] b, int sr) throws IOException {
        if (sr == SR) {
            // Effects: softened (12 ms fade-in, 2.8 kHz low-pass) and left 8 dB of headroom so
            // they sit beside the music instead of jumping out of it (music loops are normalised
            // to 0.7 and played at 0.32). Second pass of user feedback (2026-11-03): still too
            // sharp at 4 kHz / -6 dBFS on a phone speaker.
            soften(b, sr, 2800);
            normalize(b, 0.4);
        }
        ByteBuffer data = ByteBuffer.allocate(b.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (double v : b) {
            data.putShort((short) Math.round(Math.max(-1, Math.min(1, v)) * 32767));
        }
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes()).putInt(36 + data.capacity()).put("WAVE".getBytes())
              .put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1)
              .putInt(sr).putInt(sr * 2).putShort((short) 2).putShort((short) 16)
              .put("data".getBytes()).putInt(data.capacity());
        Path file = dir.resolve(name + ".wav");
        try (OutputStream os = Files.newOutputStream(file)) {
            os.write(header.array());
            os.write(data.array());
        }
        System.out.printf("%-14s %6.2f s  %5d KB%n", file.getFileName(), (double) b.length / sr,
                (44 + data.capacity()) / 1024);
    }
}
