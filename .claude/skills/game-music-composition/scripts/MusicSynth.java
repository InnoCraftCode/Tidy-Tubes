import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generic, dependency-free music loop generator for the workspace's libGDX games.
 *
 * <p>Writes seamless 16-bit mono WAV loops (and optional stems) from a style, tempo, key, mode,
 * bar count and seed. Every note wraps around the loop end, so the first bar inherits the tail
 * of the last one and the seam is inaudible. Output is peak-normalised to 0.7 so that the games'
 * {@code MUSIC_VOLUME = 0.32f} gives the same loudness as the shipped Tidy Tubes beds.</p>
 *
 * <pre>
 * java MusicSynth.java --out assets/sfx --name music --style ambient --count 5 --seed 7
 * java MusicSynth.java --out out --style chiptune --tempo 128 --key A --mode minor --bars 16 --stems
 * </pre>
 *
 * Encode the results with scripts/encode-music.ps1 (FFmpeg, libvorbis q3) before shipping.
 */
public final class MusicSynth {

    enum Wave { SINE, SINE_OCTAVE, TRIANGLE, PLUCK, SQUARE, SAW }

    /** Per-style parameters; edit here to tune a whole family of loops at once. */
    enum Style {
        //        tempo minor padAmp padWave    bassAmp melAmp melDensity melWave     pluckDecay arp   drums lowpassHz
        AMBIENT (68,  false, 0.085, Wave.SINE,     0.16, 0.065, 22, Wave.SINE_OCTAVE, 0.75, false, false, 5000),
        CHIPTUNE(128, false, 0.040, Wave.TRIANGLE, 0.14, 0.055, 45, Wave.SQUARE,      0.22, true,  true,  7000),
        UPBEAT  (112, false, 0.060, Wave.SINE,     0.17, 0.080, 35, Wave.PLUCK,       0.45, true,  true,  6500),
        TENSE   (120, true,  0.070, Wave.SAW,      0.19, 0.060, 18, Wave.PLUCK,       0.35, false, true,  6000);

        final int tempo;
        final boolean minor;
        final double padAmp;
        final Wave padWave;
        final double bassAmp;
        final double melAmp;
        final int melDensity;
        final Wave melWave;
        final double pluckDecay;
        final boolean arpeggio;
        final boolean drums;
        final double lowpassHz;

        Style(int tempo, boolean minor, double padAmp, Wave padWave, double bassAmp, double melAmp,
              int melDensity, Wave melWave, double pluckDecay, boolean arpeggio, boolean drums,
              double lowpassHz) {
            this.tempo = tempo;
            this.minor = minor;
            this.padAmp = padAmp;
            this.padWave = padWave;
            this.bassAmp = bassAmp;
            this.melAmp = melAmp;
            this.melDensity = melDensity;
            this.melWave = melWave;
            this.pluckDecay = pluckDecay;
            this.arpeggio = arpeggio;
            this.drums = drums;
            this.lowpassHz = lowpassHz;
        }
    }

    static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11};
    static final int[] MINOR_SCALE = {0, 2, 3, 5, 7, 8, 10};
    static final int[] MAJOR_PENT = {0, 2, 4, 7, 9};
    static final int[] MINOR_PENT = {0, 3, 5, 7, 10};
    /** Degree is 0-based (I = 0). Major: I ii iii IV V vi. */
    static final int[][] MAJOR_PROGRESSIONS = {
        {0, 4, 5, 3}, {0, 5, 3, 4}, {5, 3, 0, 4}, {0, 3, 1, 4}, {0, 2, 5, 3}, {3, 0, 4, 5}
    };
    /** Minor (natural): i ii III iv v VI VII. */
    static final int[][] MINOR_PROGRESSIONS = {
        {0, 5, 2, 6}, {0, 3, 6, 4}, {0, 6, 5, 6}, {0, 2, 3, 4}, {0, 5, 3, 6}, {5, 6, 0, 0}
    };
    static final boolean[] MAJOR_CHORD_IS_MAJOR = {true, false, false, true, true, false, false};
    static final boolean[] MINOR_CHORD_IS_MAJOR = {false, false, true, false, false, true, true};

    static int sampleRate = 22_050;

    public static void main(String[] args) throws IOException {
        Path out = null;
        String name = "music";
        Style style = Style.AMBIENT;
        Integer tempo = null;
        int key = 0;
        Boolean minor = null;
        int bars = 8;
        boolean barsGiven = false;
        long seed = 1;
        int count = 1;
        boolean stems = false;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--help": case "-h": usage(); return;
                case "--out": out = Paths.get(args[++i]); break;
                case "--name": name = args[++i]; break;
                case "--style": style = Style.valueOf(args[++i].toUpperCase(Locale.ROOT)); break;
                case "--tempo": tempo = Integer.parseInt(args[++i]); break;
                case "--key": key = parseKey(args[++i]); break;
                case "--mode": minor = args[++i].equalsIgnoreCase("minor"); break;
                case "--bars": bars = Integer.parseInt(args[++i]); barsGiven = true; break;
                case "--seed": seed = Long.parseLong(args[++i]); break;
                case "--count": count = Integer.parseInt(args[++i]); break;
                case "--stems": stems = true; break;
                case "--sr": sampleRate = Integer.parseInt(args[++i]); break;
                default: System.err.println("Unknown option " + a); usage(); System.exit(2);
            }
        }
        if (out == null) { usage(); System.exit(2); }
        int bpm = tempo != null ? tempo : style.tempo;
        boolean isMinor = minor != null ? minor : style.minor;
        if (!barsGiven && bpm >= 100) bars = 16;   // keeps fast loops in the 20-40 s window
        if (bpm < 40 || bpm > 200 || bars < 2 || bars > 64 || count < 1 || count > 99) {
            System.err.println("tempo 40-200, bars 2-64, count 1-99");
            System.exit(2);
        }
        Files.createDirectories(out);
        System.out.printf(Locale.ROOT, "style=%s tempo=%d key=%s %s bars=%d sr=%d%n",
                style.name().toLowerCase(Locale.ROOT), bpm, NOTE_NAMES[key],
                isMinor ? "minor" : "major", bars, sampleRate);
        for (int v = 0; v < count; v++) {
            String base = count > 1 ? String.format(Locale.ROOT, "%s_%02d", name, v + 1) : name;
            Composition c = compose(style, bpm, key, isMinor, bars, seed + v);
            write(out, base, c.mix);
            if (stems) {
                write(out, base + "_pad", c.pad);
                write(out, base + "_bass", c.bass);
                write(out, base + "_melody", c.melody);
                write(out, base + "_drums", c.drums);
            }
        }
    }

    static void usage() {
        System.out.println("Usage: java MusicSynth.java --out <dir> [--name music] [--style ambient|chiptune|upbeat|tense]\n"
                + "       [--tempo bpm] [--key C|C#|Db|D|...|B] [--mode major|minor] [--bars 8] [--seed 1]\n"
                + "       [--count 1] [--stems] [--sr 22050]\n"
                + "Writes <name>.wav (or <name>_01.wav ... with --count), 16-bit mono, peak 0.7, seamless loop.");
    }

    static int parseKey(String s) {
        String k = s.trim();
        String letter = k.substring(0, 1).toUpperCase(Locale.ROOT);
        int base;
        switch (letter) {
            case "C": base = 0; break;
            case "D": base = 2; break;
            case "E": base = 4; break;
            case "F": base = 5; break;
            case "G": base = 7; break;
            case "A": base = 9; break;
            case "B": base = 11; break;
            default: throw new IllegalArgumentException("unknown key " + s);
        }
        for (int i = 1; i < k.length(); i++) {
            char c = k.charAt(i);
            if (c == '#') base++;
            else if (c == 'b' || c == 'B') base--;
            else throw new IllegalArgumentException("unknown key " + s);
        }
        return Math.floorMod(base, 12);
    }

    // ------------------------------------------------------------------------------ composition

    static final class Composition {
        double[] pad, bass, melody, drums, mix;
    }

    static Composition compose(Style st, int bpm, int key, boolean minor, int bars, long seed) {
        int sr = sampleRate;
        Random r = new Random(seed * 7919L + 17);
        double beat = 60.0 / bpm;
        double bar = beat * 4;
        int n = samples(bar * bars);
        Composition c = new Composition();
        c.pad = new double[n];
        c.bass = new double[n];
        c.melody = new double[n];
        c.drums = new double[n];

        int[] scale = minor ? MINOR_SCALE : MAJOR_SCALE;
        int[] pent = minor ? MINOR_PENT : MAJOR_PENT;
        boolean[] chordMajor = minor ? MINOR_CHORD_IS_MAJOR : MAJOR_CHORD_IS_MAJOR;
        int[][] pool = minor ? MINOR_PROGRESSIONS : MAJOR_PROGRESSIONS;
        int[] progA = pool[r.nextInt(pool.length)];
        int[] progB = bars >= 16 || r.nextInt(3) == 0 ? pool[r.nextInt(pool.length)] : progA;
        int rootMidi = 48 + key;

        // Two-bar melodic phrases, arranged A A' B A' ... (repetition with variation reads as a tune).
        int[] phraseA = phrase(r, pent, st.melDensity);
        int[] phraseA2 = vary(r, phraseA, pent, st.melDensity);
        int[] phraseB = phrase(r, pent, st.melDensity);
        int[][] arrangement = {phraseA, phraseA2, phraseB, phraseA2};

        for (int b = 0; b < bars; b++) {
            double t0 = b * bar;
            int[] prog = (b / 4) % 2 == 0 ? progA : progB;
            int degree = prog[b % 4];
            int root = rootMidi + scale[degree];
            int third = root + (chordMajor[degree] ? 4 : 3);
            int fifth = root + 7;

            // Pad: chord tones one octave up, overlapping the neighbouring bars.
            for (int note : new int[] {root + 12, third + 12, fifth + 12}) {
                pad(c.pad, t0 - 0.2, bar + 0.6, midi(note), st.padAmp, st.padWave);
            }

            // Bass.
            double bassF = midi(root - 12);
            switch (st) {
                case CHIPTUNE:
                    for (int e = 0; e < 8; e++) {
                        pluck(c.bass, t0 + e * beat / 2, e % 2 == 0 ? bassF : bassF * 2,
                                st.bassAmp * (e % 2 == 0 ? 1 : 0.6), 0.18, Wave.TRIANGLE);
                    }
                    break;
                case TENSE:
                    pluck(c.bass, t0, bassF, st.bassAmp, 1.3, Wave.SAW);
                    pluck(c.bass, t0 + 3.5 * beat, bassF, st.bassAmp * 0.7, 0.4, Wave.SAW);
                    break;
                case UPBEAT:
                    for (int q = 0; q < 4; q++) {
                        pluck(c.bass, t0 + q * beat, q == 2 ? midi(fifth - 12) : bassF,
                                st.bassAmp * (q == 0 ? 1 : 0.75), 0.5, Wave.SINE);
                    }
                    break;
                default:
                    pluck(c.bass, t0, bassF, st.bassAmp, 1.15, Wave.SINE);
                    if (r.nextInt(3) != 0) {
                        pluck(c.bass, t0 + 2 * beat, bassF, st.bassAmp * 0.7, 0.9, Wave.SINE);
                    }
            }

            // Melody from the arranged phrases (16 eighth slots per two bars).
            int[] ph = arrangement[(b / 2) % arrangement.length];
            int slotOffset = (b % 2) * 8;
            for (int e = 0; e < 8; e++) {
                int note = ph[slotOffset + e];
                if (note < 0) continue;
                pluck(c.melody, t0 + e * beat / 2, midi(rootMidi + 12 + note), st.melAmp,
                        st.pluckDecay, st.melWave);
            }

            // Arpeggio on the second half of each four-bar group, so the texture breathes.
            if (st.arpeggio && b % 4 >= 2) {
                int[] tones = {root + 12, third + 12, fifth + 12, root + 24};
                for (int s = 0; s < 16; s++) {
                    pluck(c.melody, t0 + s * beat / 4, midi(tones[s % 4]), st.melAmp * 0.45, 0.12,
                            st == Style.CHIPTUNE ? Wave.SQUARE : Wave.SINE_OCTAVE);
                }
            }

            // Drums.
            if (st.drums) {
                for (int q = 0; q < 4; q++) {
                    boolean kickHere = st == Style.TENSE || q % 2 == 0;
                    if (kickHere) kick(c.drums, t0 + q * beat, 0.25);
                    if (st == Style.UPBEAT && q % 2 == 1) snare(c.drums, t0 + q * beat, 0.12, seed + b * 4 + q);
                    hat(c.drums, t0 + q * beat + beat / 2, st == Style.TENSE ? 0.035 : 0.05, seed + 100 + b * 4 + q);
                }
            }
        }

        // Mix, normalise the sum to 0.7 and apply the same gain to every stem.
        c.mix = new double[n];
        double max = 1e-9;
        for (int i = 0; i < n; i++) {
            c.mix[i] = c.pad[i] + c.bass[i] + c.melody[i] + c.drums[i];
            max = Math.max(max, Math.abs(c.mix[i]));
        }
        double gain = 0.7 / max;
        for (double[] layer : new double[][] {c.pad, c.bass, c.melody, c.drums, c.mix}) {
            lowpass(layer, st.lowpassHz);
            for (int i = 0; i < n; i++) layer[i] *= gain;
        }
        return c;
    }

    /** 16 eighth-note slots: -1 = rest, else a pentatonic offset in semitones (0-24). */
    static int[] phrase(Random r, int[] pent, int density) {
        int[] p = new int[16];
        int last = pent[r.nextInt(pent.length)];
        for (int s = 0; s < 16; s++) {
            int threshold = s % 2 == 0 ? density : density / 3;
            if (s == 0 || r.nextInt(100) < threshold) {
                // Step-wise motion most of the time: walk to a neighbouring pentatonic degree.
                int idx = indexOf(pent, last % 12);
                int move = r.nextInt(5) - 2;             // -2..+2 degrees
                int ni = Math.floorMod(idx + move, pent.length);
                int octave = (idx + move) < 0 ? -12 : (idx + move) >= pent.length ? 12 : 0;
                int note = Math.max(0, Math.min(24, last - (last % 12) + octave + pent[ni]));
                p[s] = note;
                last = note;
            } else {
                p[s] = -1;
            }
        }
        return p;
    }

    static int[] vary(Random r, int[] base, int[] pent, int density) {
        int[] v = base.clone();
        int[] tail = phrase(r, pent, density);
        System.arraycopy(tail, 12, v, 12, 4);
        return v;
    }

    static int indexOf(int[] a, int v) {
        for (int i = 0; i < a.length; i++) if (a[i] == v) return i;
        return 0;
    }

    // ---------------------------------------------------------------------------- synthesis kit

    static double osc(Wave w, double phase, double t, double f) {
        switch (w) {
            case SINE_OCTAVE:
                return Math.sin(phase) + 0.25 * Math.sin(2 * phase);
            case TRIANGLE:
                return 2 / Math.PI * Math.asin(Math.sin(phase));
            case PLUCK:
                return Math.sin(phase) + 0.35 * Math.sin(2 * phase) * Math.exp(-t * 18)
                        + 0.15 * Math.sin(3 * phase) * Math.exp(-t * 30);
            case SQUARE: {
                // Band-limited: odd harmonics below Nyquist, at most 9 of them.
                double v = 0;
                for (int k = 1; k <= 17 && k * f < sampleRate / 2.0; k += 2) v += Math.sin(k * phase) / k;
                return v * 4 / Math.PI * 0.6;
            }
            case SAW: {
                double v = 0;
                for (int k = 1; k <= 12 && k * f < sampleRate / 2.0; k++) v += Math.sin(k * phase) / k;
                return v * 2 / Math.PI * 0.7;
            }
            default:
                return Math.sin(phase);
        }
    }

    /** Slow-attack pad; samples past the loop end wrap round to the start. */
    static void pad(double[] b, double start, double dur, double f, double amp, Wave w) {
        int s0 = samples(start);
        int n = samples(dur);
        double p1 = 0;
        double p2 = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            p1 += 2 * Math.PI * f * 1.003 / sampleRate;
            p2 += 2 * Math.PI * f * 0.997 / sampleRate;
            double env = Math.pow(Math.sin(Math.PI * t), 1.5);
            double v = (osc(w, p1, t, f) + osc(w, p2, t, f)) * 0.5;
            b[Math.floorMod(s0 + i, b.length)] += amp * env * v;
        }
    }

    /** Plucked note with exponential decay; wraps round the loop end. */
    static void pluck(double[] b, double start, double f, double amp, double decay, Wave w) {
        int s0 = samples(start);
        int n = samples(decay * 5);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / sampleRate;
            phase += 2 * Math.PI * f / sampleRate;
            double env = Math.min(1, t / 0.004) * Math.exp(-t / decay) * fadeTail(i, n);
            b[Math.floorMod(s0 + i, b.length)] += amp * env * osc(w, phase, t, f);
        }
    }

    /** Sine sweep 120 -> 45 Hz: a soft kick. */
    static void kick(double[] b, double start, double amp) {
        int s0 = samples(start);
        int n = samples(0.18);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / sampleRate;
            double f = 120 * Math.pow(45.0 / 120, t / 0.18);
            phase += 2 * Math.PI * f / sampleRate;
            double env = Math.min(1, t / 0.003) * Math.exp(-t / 0.07) * fadeTail(i, n);
            b[Math.floorMod(s0 + i, b.length)] += amp * env * Math.sin(phase);
        }
    }

    static void snare(double[] b, double start, double amp, long seed) {
        noise(b, start, 0.12, amp, 0.25, seed);
        pluck(b, start, 180, amp * 0.8, 0.05, Wave.SINE);
    }

    static void hat(double[] b, double start, double amp, long seed) {
        noise(b, start, 0.03, amp, 0.7, seed);
    }

    /** One-pole low-passed noise burst with a linear decay; wraps round the loop end. */
    static void noise(double[] b, double start, double dur, double amp, double cutoff, long seed) {
        Random r = new Random(seed);
        int s0 = samples(start);
        int n = samples(dur);
        double low = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / sampleRate;
            low += cutoff * ((r.nextDouble() * 2 - 1) - low);
            double env = Math.min(1, t / 0.002) * (1 - (double) i / n);
            b[Math.floorMod(s0 + i, b.length)] += amp * low * env;
        }
    }

    /** Two cascaded one-pole low-pass filters, run twice around the loop so the seam stays filtered. */
    static void lowpass(double[] b, double cutoffHz) {
        double a = 1 - Math.exp(-2 * Math.PI * cutoffHz / sampleRate);
        double y1 = 0;
        double y2 = 0;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < b.length; i++) {
                y1 += a * (b[i] - y1);
                y2 += a * (y1 - y2);
                if (pass == 1) b[i] = y2;
            }
        }
    }

    static double fadeTail(int i, int n) {
        int fade = Math.min(n / 4, 220);
        return i > n - fade ? (double) (n - i) / fade : 1;
    }

    static double midi(int note) {
        return 440.0 * Math.pow(2, (note - 69) / 12.0);
    }

    static int samples(double seconds) {
        return (int) Math.round(seconds * sampleRate);
    }

    // ------------------------------------------------------------------------------------ output

    static void write(Path dir, String name, double[] b) throws IOException {
        int sr = sampleRate;
        ByteBuffer data = ByteBuffer.allocate(b.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        double peak = 0;
        for (double v : b) {
            double c = Math.max(-1, Math.min(1, v));
            peak = Math.max(peak, Math.abs(c));
            data.putShort((short) Math.round(c * 32767));
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
        // Seam = jump across the loop point relative to the signal's own sample-to-sample motion,
        // so a bright saw pad (large normal jumps) is not flagged while a real discontinuity is.
        double step = 1e-9;
        for (int i = 1; i < b.length; i++) step += Math.abs(b[i] - b[i - 1]);
        step /= b.length - 1;
        double seam = Math.abs(b[0] - b[b.length - 1]) / step;
        System.out.printf(Locale.ROOT, "%-22s %6.2f s %6d KB  peak %5.2f  seam x%.1f%s%n", file.getFileName(),
                (double) b.length / sr, (44 + data.capacity()) / 1024, peak, seam,
                seam > 4 ? "  <- audible seam, try another seed" : "");
    }
}
