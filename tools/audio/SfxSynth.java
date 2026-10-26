import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Synthesises the game's sound effects and a loopable music bed as 16-bit mono WAV files.
 *
 * <p>Everything is generated from code (sine/triangle oscillators, bell partials, filtered
 * noise), so the audio is original and free of third-party licences. Deterministic: the same
 * run always produces the same files.</p>
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
        write(out, "music", music(), MUSIC_SR);
    }

    // ------------------------------------------------------------------------------- sounds

    /** Ball lifted: a short rising "bloop". */
    static double[] pick() {
        double[] b = new double[samples(0.16, SR)];
        sweep(b, SR, 0, 0.13, 380, 760, 0.8, 0.004, 0.05, Wave.SINE_OCTAVE);
        noise(b, SR, 0, 0.006, 0.15, 0.002, 0.7, 7);
        return b;
    }

    /** Ball lands: glassy clink with a soft knock underneath. Pitched up per slot in code. */
    static double[] drop() {
        double[] b = new double[samples(0.5, SR)];
        bell(b, SR, 0.0, 1046.5, 0.7, 0.22);
        sweep(b, SR, 0, 0.06, 240, 140, 0.45, 0.002, 0.02, Wave.TRIANGLE);
        return b;
    }

    /** Illegal move: low, soft double "bonk" - clearly "no" without being harsh. */
    static double[] invalid() {
        double[] b = new double[samples(0.3, SR)];
        sweep(b, SR, 0, 0.24, 196, 160, 0.55, 0.004, 0.09, Wave.TRIANGLE);
        sweep(b, SR, 0, 0.24, 207, 168, 0.45, 0.004, 0.09, Wave.TRIANGLE);
        noise(b, SR, 0, 0.05, 0.25, 0.003, 0.08, 11);
        return b;
    }

    /** UI tap: tiny wooden tick. */
    static double[] button() {
        double[] b = new double[samples(0.07, SR)];
        sweep(b, SR, 0, 0.05, 1500, 1150, 0.7, 0.001, 0.014, Wave.SINE);
        sweep(b, SR, 0, 0.02, 3000, 2800, 0.2, 0.001, 0.006, Wave.SINE);
        return b;
    }

    /** Coins: the classic two-note "ka-ching" as bells (E6 then B6). */
    static double[] coin() {
        double[] b = new double[samples(0.75, SR)];
        bell(b, SR, 0.000, 1318.5, 0.55, 0.12);
        bell(b, SR, 0.075, 1975.5, 0.8, 0.35);
        return b;
    }

    /** A tube finished: bright rising arpeggio with a sparkle on top. */
    static double[] tube() {
        double[] b = new double[samples(1.3, SR)];
        double[] notes = {1046.5, 1318.5, 1568.0, 2093.0};
        for (int i = 0; i < notes.length; i++) {
            bell(b, SR, i * 0.06, notes[i], 0.75 - i * 0.1, 0.45);
        }
        sparkle(b, SR, 0.18, 0.9, 10, 0.12, 21);
        return b;
    }

    /** Hint shown: gentle shimmering arpeggio. */
    static double[] hint() {
        double[] b = new double[samples(1.0, SR)];
        double[] notes = {783.99, 987.77, 1174.66, 1567.98};
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
            double cutoff = 0.02 + 0.25 * t;            // filter opens as the whoosh rises
            low += cutoff * ((r.nextDouble() * 2 - 1) - low);
            b[i] += low * Math.sin(Math.PI * t) * 0.9;
        }
        sweep(b, SR, 0.05, 0.4, 300, 900, 0.15, 0.1, 0.2, Wave.SINE);
        return b;
    }

    /** Level complete: plucked arpeggio up to a ringing major chord with sparkles. */
    static double[] win() {
        double[] b = new double[samples(2.0, SR)];
        double[] arp = {523.25, 659.25, 783.99, 1046.5, 1318.5};
        for (int i = 0; i < arp.length; i++) {
            sweep(b, SR, i * 0.09, 0.3, arp[i], arp[i], 0.6, 0.003, 0.12, Wave.PLUCK);
        }
        double[] chord = {523.25, 659.25, 783.99, 1046.5};
        for (double f : chord) {
            bell(b, SR, 0.45, f, 0.5, 0.9);
        }
        sparkle(b, SR, 0.45, 1.4, 16, 0.12, 33);
        return b;
    }

    /**
     * 20-second music bed (8 bars at 96 bpm): soft pads, a round bass and a sparse music-box
     * melody. Notes that ring past the end wrap to the start, so the loop is seamless.
     */
    static double[] music() {
        int sr = MUSIC_SR;
        double beat = 60.0 / 96.0;
        double bar = beat * 4;
        double[] b = new double[samples(bar * 8, sr)];
        // C, Am, F, G, C, Am, Dm, G - as MIDI note numbers (root, third, fifth).
        int[][] chords = {{48, 52, 55}, {45, 48, 52}, {41, 45, 48}, {43, 47, 50},
                          {48, 52, 55}, {45, 48, 52}, {50, 53, 57}, {43, 47, 50}};
        int[] pentatonic = {72, 74, 76, 79, 81, 84};
        Random r = new Random(96);
        for (int c = 0; c < chords.length; c++) {
            double t0 = c * bar;
            for (int note : chords[c]) {
                double f = midi(note + 12);
                padWrapped(b, sr, t0 - 0.2, bar + 0.6, f, 0.10);
            }
            double root = midi(chords[c][0] - 12);
            pluckWrapped(b, sr, t0, root, 0.32, 0.9, Wave.SINE_OCTAVE);
            pluckWrapped(b, sr, t0 + 2 * beat, root, 0.25, 0.7, Wave.SINE_OCTAVE);
            for (int eighth = 0; eighth < 8; eighth++) {
                if (r.nextDouble() < (eighth % 2 == 0 ? 0.45 : 0.2)) {
                    int note = pentatonic[r.nextInt(pentatonic.length)];
                    pluckWrapped(b, sr, t0 + eighth * beat / 2, midi(note), 0.13, 0.45, Wave.PLUCK);
                }
            }
        }
        normalize(b, 0.8);
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
        double[] level = {1.0, 0.45, 0.3, 0.12};
        double[] life = {1.0, 0.7, 0.45, 0.25};
        for (int p = 0; p < ratio.length; p++) {
            if (f * ratio[p] > sr / 2.2) {
                continue; // stay below Nyquist
            }
            sweep(b, sr, start, decay * 5, f * ratio[p], f * ratio[p], amp * level[p], 0.002,
                    decay * life[p], Wave.SINE);
        }
    }

    /** Random high pings, like glitter. */
    static void sparkle(double[] b, int sr, double from, double to, int count, double amp, long seed) {
        Random r = new Random(seed);
        for (int i = 0; i < count; i++) {
            double t = from + r.nextDouble() * (to - from);
            double f = 2200 + r.nextDouble() * 2800;
            sweep(b, sr, t, 0.25, f, f, amp * (0.4 + 0.6 * r.nextDouble()), 0.002, 0.05, Wave.SINE);
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
            normalize(b, 0.89);
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
