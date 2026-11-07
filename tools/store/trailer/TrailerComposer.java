import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

/**
 * Cinematic trailer editor for the workspace's games: cuts real gameplay scenes (filmed by
 * StoreCapture in trailer mode, with a log of every sound cue) into a beat-synced trailer with
 * camera moves, animated titles, particles, a logo sting, narration, music and sound design.
 *
 * <pre>
 * java TrailerComposer.java --scenes DIR --lang en|fr|ar --cut main|short --format land|port
 *      --music DIR --sfx DIR --fonts DIR --icon PNG --ffmpeg EXE --out MP4 [--voice DIR] [--threads N]
 * </pre>
 *
 * Everything is deterministic (fixed seeds): the same inputs give the same video. The audio is
 * mixed here (48 kHz stereo), then FFmpeg grades the picture lightly, normalises loudness to
 * -14 LUFS and encodes H.264/AAC in a single pass.
 */
public final class TrailerComposer {

    static final int FPS = 30;
    static final int RATE = 48000;
    static final double BAR = 60.0 / 112.0 * 4.0; // music: 112 BPM, 4/4
    static final double SRC_ASPECT = 1620.0 / 2880.0;

    static double bar(double n) {
        return n * BAR;
    }

    // ------------------------------------------------------------------ palette
    static final Color BG_TOP = new Color(0x222B5E);
    static final Color BG_BOTTOM = new Color(0x0B0E24);
    static final Color AURORA_A = new Color(0x3FB7C9);
    static final Color AURORA_B = new Color(0x7B5CE6);
    static final Color PRIMARY = new Color(0x4FE3C1);
    static final Color ON_PRIMARY = new Color(0x05221B);
    static final Color TEXT = new Color(0xF4F6FF);
    static final Color[] BALLS = {new Color(0xFF5A5F), new Color(0x4D8BFF), new Color(0xFFD34D),
        new Color(0x45D483), new Color(0xB16CFF), new Color(0xFF9F43), new Color(0x4FE3C1), new Color(0xFF7EB6)};

    // ------------------------------------------------------------------ options
    final Path scenesDir;
    final Path voiceDir;
    final Path musicDir;
    final Path sfxDir;
    final Path fontsDir;
    final Path iconFile;
    final String ffmpeg;
    final Path out;
    final String lang;
    final String cut;
    final boolean land;
    final int threads;
    final int W;
    final int H;
    final double kCover;

    final Map<String, Scene> scenes = new HashMap<>();
    final Map<String, String> copy;
    final Map<String, Double> voiceLength = new HashMap<>();
    final List<Shot> shots = new ArrayList<>();
    final List<Title> titles = new ArrayList<>();
    final List<Caption> captions = new ArrayList<>();
    final List<Double> flashes = new ArrayList<>();
    final List<double[]> letterbox = new ArrayList<>(); // {start, end}
    final List<Sound> sounds = new ArrayList<>();
    final List<MusicCue> music = new ArrayList<>();
    double total;

    Font titleFont;
    Font lineFont;
    Font arabicTitle;
    Font arabicLine;
    BufferedImage icon;
    BufferedImage background;
    BufferedImage vignette;
    BufferedImage[] glow;

    public static void main(String[] args) throws Exception {
        Map<String, String> a = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            a.put(args[i].replaceFirst("^--", ""), args[i + 1]);
        }
        TrailerComposer composer = new TrailerComposer(a);
        composer.run();
    }

    TrailerComposer(Map<String, String> a) {
        scenesDir = Path.of(a.get("scenes"));
        voiceDir = a.containsKey("voice") ? Path.of(a.get("voice")) : null;
        musicDir = Path.of(a.get("music"));
        sfxDir = Path.of(a.get("sfx"));
        fontsDir = Path.of(a.get("fonts"));
        iconFile = Path.of(a.get("icon"));
        ffmpeg = a.get("ffmpeg");
        out = Path.of(a.get("out"));
        lang = a.getOrDefault("lang", "en");
        cut = a.getOrDefault("cut", "main");
        land = !"port".equals(a.getOrDefault("format", "land"));
        threads = Integer.parseInt(a.getOrDefault("threads", String.valueOf(Math.max(2, Runtime.getRuntime().availableProcessors() - 2))));
        W = land ? 1920 : 1080;
        H = land ? 1080 : 1920;
        kCover = Math.max(1.0, ((double) W / H) / SRC_ASPECT);
        copy = Copy.of(lang);
    }

    void run() throws Exception {
        long started = System.currentTimeMillis();
        loadScenes();
        loadVoiceLengths();
        loadAssets();
        if ("short".equals(cut)) {
            buildShort();
        } else {
            buildMain();
        }
        System.out.printf(Locale.ROOT, "[trailer] %s %s %s: %d shots, %.2f s%n", lang, cut, land ? "16:9" : "9:16", shots.size(), total);
        Path mix = Files.createTempFile("trailer-mix", ".wav");
        mixAudio(mix);
        renderVideo(mix);
        Files.deleteIfExists(mix);
        System.out.printf(Locale.ROOT, "[trailer] %s in %.0f s%n", out, (System.currentTimeMillis() - started) / 1000.0);
    }

    // =================================================================== inputs

    static final class Scene {
        String name;
        Path dir;
        int frames;
        final List<Object[]> events = new ArrayList<>(); // {frame, cue, pitch}

        int first(String cue, int from) {
            for (Object[] e : events) {
                if (e[1].equals(cue) && (int) e[0] >= from) return (int) e[0];
            }
            return -1;
        }
    }

    void loadScenes() throws IOException {
        try (var dirs = Files.list(scenesDir)) {
            for (Path dir : (Iterable<Path>) dirs.filter(Files::isDirectory)::iterator) {
                Scene s = new Scene();
                s.name = dir.getFileName().toString();
                s.dir = dir;
                try (var files = Files.list(dir)) {
                    s.frames = (int) files.filter(p -> p.toString().endsWith(".png")).count();
                }
                Path ev = dir.resolve("events.txt");
                if (Files.exists(ev)) {
                    for (String line : Files.readAllLines(ev)) {
                        String[] p = line.trim().split(" ");
                        if (p.length == 3) s.events.add(new Object[] {Integer.parseInt(p[0]), p[1], Double.parseDouble(p[2])});
                    }
                }
                scenes.put(s.name, s);
            }
        }
        for (String need : new String[] {"00-mess", "02-solve", "03-ten-colours", "04-shop", "05-goals", "06-music", "07-languages"}) {
            if (!scenes.containsKey(need)) throw new IllegalStateException("missing scene " + need + " in " + scenesDir);
        }
    }

    void loadVoiceLengths() throws IOException {
        if (voiceDir == null) return;
        for (String line : Files.readAllLines(voiceDir.resolve("durations.txt"))) {
            String[] p = line.trim().split(" ");
            if (p.length == 2) voiceLength.put(p[0], Double.parseDouble(p[1]));
        }
    }

    void loadAssets() throws Exception {
        titleFont = Font.createFont(Font.TRUETYPE_FONT, fontsDir.resolve("Poppins-ExtraBold.ttf").toFile());
        lineFont = Font.createFont(Font.TRUETYPE_FONT, fontsDir.resolve("Poppins-SemiBold.ttf").toFile());
        arabicTitle = Font.createFont(Font.TRUETYPE_FONT, fontsDir.resolve("NotoSansArabic-ExtraBold.ttf").toFile());
        arabicLine = Font.createFont(Font.TRUETYPE_FONT, fontsDir.resolve("NotoSansArabic-SemiBold.ttf").toFile());

        // Rounded icon with anti-aliased corners (Play's 512 icon is a full-bleed square).
        BufferedImage raw = ImageIO.read(iconFile.toFile());
        icon = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        hints(g);
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(0, 0, raw.getWidth(), raw.getHeight(), raw.getWidth() * 0.43, raw.getHeight() * 0.43));
        g.setComposite(AlphaComposite.SrcIn);
        g.drawImage(raw, 0, 0, null);
        g.dispose();

        background = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        g = background.createGraphics();
        g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, H, BG_BOTTOM));
        g.fillRect(0, 0, W, H);
        g.dispose();

        vignette = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        g = vignette.createGraphics();
        float r = (float) Math.hypot(W, H) * 0.62f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(W / 2f, H / 2f), r, new float[] {0f, 0.55f, 1f},
            new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 30), new Color(0, 0, 0, 150)}));
        g.fillRect(0, 0, W, H);
        g.dispose();

        glow = new BufferedImage[] {softDisc(AURORA_A, 512), softDisc(AURORA_B, 512), softDisc(Color.WHITE, 128)};
    }

    static BufferedImage softDisc(Color c, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new RadialGradientPaint(new Point2D.Float(size / 2f, size / 2f), size / 2f, new float[] {0f, 0.4f, 1f},
            new Color[] {new Color(c.getRed(), c.getGreen(), c.getBlue(), 255), new Color(c.getRed(), c.getGreen(), c.getBlue(), 110),
                new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
        g.fillRect(0, 0, size, size);
        g.dispose();
        return img;
    }

    // =================================================================== model

    enum Kind { CARD, LOGO_IN, LOGO_OUT }

    enum Trans { CUT, WHIP, FADE, DIP }

    /** Camera: card height k (x viewport height), source point (u,v) placed at anchor (ax,ay). */
    static final class Cam {
        double k, u, v, ax, ay, rot;

        Cam(double k, double u, double v, double ax, double ay, double rot) {
            this.k = k; this.u = u; this.v = v; this.ax = ax; this.ay = ay; this.rot = rot;
        }

        static Cam lerp(Cam a, Cam b, double t) {
            // Zoom interpolates geometrically, so a push-in feels even.
            return new Cam(Math.exp(Math.log(a.k) + (Math.log(b.k) - Math.log(a.k)) * t), a.u + (b.u - a.u) * t,
                a.v + (b.v - a.v) * t, a.ax + (b.ax - a.ax) * t, a.ay + (b.ay - a.ay) * t, a.rot + (b.rot - a.rot) * t);
        }
    }

    static final class Shot {
        Kind kind = Kind.CARD;
        double start, end;
        Scene scene;
        double srcStart, srcEnd; // frames
        boolean easeOutTime;     // speed ramp: fast, slowing into the end
        final List<double[]> keyTimes = new ArrayList<>();
        final List<Cam> keys = new ArrayList<>();
        Trans in = Trans.CUT;

        double dur() {
            return end - start;
        }

        Shot key(double at, Cam cam) {
            keyTimes.add(new double[] {at});
            keys.add(cam);
            return this;
        }

        Cam cam(double x) {
            if (keys.size() == 1) return keys.get(0);
            for (int i = 0; i + 1 < keys.size(); i++) {
                double a = keyTimes.get(i)[0], b = keyTimes.get(i + 1)[0];
                if (x <= b || i + 2 == keys.size()) {
                    double t = clamp((x - a) / Math.max(1e-6, b - a), 0, 1);
                    return Cam.lerp(keys.get(i), keys.get(i + 1), easeInOutCubic(t));
                }
            }
            return keys.get(keys.size() - 1);
        }

        double timeMap(double x) {
            return easeOutTime ? 1 - (1 - x) * (1 - x) : x;
        }

        double timeMapInverse(double y) {
            return easeOutTime ? 1 - Math.sqrt(Math.max(0, 1 - y)) : y;
        }

        int sourceFrame(double local) {
            double x = clamp(local / dur(), 0, 1);
            int f = (int) Math.floor(srcStart + (srcEnd - srcStart) * timeMap(x));
            return Math.max(0, Math.min(scene.frames - 1, f));
        }
    }

    static final class Title {
        double start, end;
        String title, line;
    }

    static final class Caption {
        double start, end;
        String text;
    }

    static final class Sound {
        double at, gain, pan, pitch = 1;
        float[][] data;
        boolean voice;
    }

    static final class MusicCue {
        float[][] data;
        double at, from, length, fadeIn, fadeOut, gain;
    }

    // ------------------------------------------------------------------ helpers for the cuts

    /** Card resting position: right half on 16:9, below the titles on 9:16. */
    Cam card(double scale, double u, double v) {
        return land ? new Cam(0.9 * scale, u, v, 0.70, 0.5 + (v - 0.5) * 0.0, 0)
            : new Cam(0.79 * scale, u, v, 0.5, 0.585 + (v - 0.5) * 0.0, 0);
    }

    Cam card() {
        return card(1, 0.5, 0.5);
    }

    /** Full-bleed close-up: z = 1 just covers the frame. */
    Cam macro(double z, double u, double v, double rot) {
        return new Cam(kCover * z, u, v, 0.5, 0.5, rot);
    }

    double macroZ() {
        return land ? 1.22 : 2.1;
    }

    Shot shot(String scene, double start, double end, double srcStart, double srcEnd, Trans in) {
        Shot s = new Shot();
        s.scene = scenes.get(scene);
        s.start = start;
        s.end = end;
        s.srcStart = srcStart;
        s.srcEnd = Math.min(srcEnd, s.scene.frames - 1);
        s.in = in;
        shots.add(s);
        return s;
    }

    Shot logo(Kind kind, double start, double end, Trans in) {
        Shot s = new Shot();
        s.kind = kind;
        s.start = start;
        s.end = end;
        s.in = in;
        s.key(0, card());
        shots.add(s);
        return s;
    }

    void title(double start, double end, String key) {
        Title t = new Title();
        t.start = start;
        t.end = end;
        t.title = copy.get(key + ".t");
        t.line = copy.get(key + ".l");
        titles.add(t);
    }

    void caption(double start, double length, String key) {
        Caption c = new Caption();
        c.start = start;
        c.end = start + length;
        c.text = copy.get(key);
        captions.add(c);
    }

    /** Places a narration line (if this language has a voice) and returns its length. */
    double voice(String id, double at, double fallback) throws IOException {
        if (voiceDir == null || !voiceLength.containsKey(id)) return fallback;
        Sound s = new Sound();
        s.at = at;
        s.gain = 1.0;
        s.voice = true;
        s.data = decode(voiceDir.resolve(id + ".wav"));
        sounds.add(s);
        return voiceLength.get(id);
    }

    void sfx(float[][] data, double at, double gain, double pan) {
        Sound s = new Sound();
        s.data = data;
        s.at = at;
        s.gain = gain;
        s.pan = pan;
        sounds.add(s);
    }

    void music(String file, double at, double from, double length, double fadeIn, double fadeOut, double gain) throws IOException {
        MusicCue m = new MusicCue();
        m.data = decode(musicDir.resolve(file));
        m.at = at;
        m.from = from;
        m.length = length;
        m.fadeIn = fadeIn;
        m.fadeOut = fadeOut;
        m.gain = gain;
        music.add(m);
    }

    // =================================================================== the cuts

    void buildMain() throws IOException {
        Scene solve = scenes.get("02-solve");
        Scene ten = scenes.get("03-ten-colours");
        int p0 = Math.max(0, solve.first("PICK", 0));
        int win = solve.first("WIN", p0);
        if (win < 0) win = solve.frames - 75;
        int hint = ten.first("HINT", 0);
        double z = macroZ();

        // ---- Cold open: mystery, letterboxed, narration over close-ups of the tangled board.
        letterbox.add(new double[] {0, bar(5)});
        Shot s;
        s = shot("00-mess", 0, bar(1.5), 0, 45, Trans.DIP);
        s.key(0, macro(z * 1.12, 0.30, 0.30, 0)).key(1, macro(z, 0.38, 0.33, 0));
        s = shot("00-mess", bar(1.5), bar(3), 50, 100, Trans.WHIP);
        s.key(0, macro(z * 1.15, 0.68, 0.66, -1.5)).key(1, macro(z, 0.60, 0.64, 0));
        s = shot("00-mess", bar(3), bar(4), 100, 149, Trans.FADE);
        s.key(0, macro(z * 0.95, 0.5, 0.45, 0)).key(1, card());
        s = shot("02-solve", bar(4), bar(5), p0 - 6, p0 - 6 + 64, Trans.WHIP);
        s.key(0, macro(z * 0.7, 0.5, 0.40, 0)).key(1, macro(z * 0.82, 0.5, 0.36, 0));

        double t1 = 0.55, t2 = bar(1.5) + 0.3, t3 = bar(3) + 0.25, t4 = bar(4) + 0.55;
        caption(t1, voice("m01", t1, 2.0), "m01");
        caption(t2, voice("m02", t2, 1.9), "m02");
        caption(t3, voice("m03", t3, 1.6), "m03");
        caption(t4, voice("m04", t4, 1.3), "m04");

        // ---- The drop: logo sting.
        flashes.add(bar(5));
        logo(Kind.LOGO_IN, bar(5), bar(6.5), Trans.CUT);
        voice("m05", bar(5) + 0.45, 1.0);

        // ---- Gameplay acts.
        s = shot("02-solve", bar(6.5), bar(9.5), p0, win, Trans.WHIP);
        s.easeOutTime = true;
        s.key(0, card(1.4, 0.5, 0.36)).key(0.45, card()).key(1, card(1.06, 0.5, 0.45));
        title(bar(6.5) + 0.25, bar(9.5) - 0.1, "sort");
        voice("m06", bar(6.5) + 0.3, 3);

        s = shot("02-solve", bar(9.5), bar(10.5), win, win + 64, Trans.CUT);
        s.key(0, card(1.0, 0.5, 0.4)).key(1, card(1.3, 0.5, 0.3));

        double tenDur = bar(3);
        s = shot("03-ten-colours", bar(10.5), bar(13.5), 0, ten.frames - 1, Trans.WHIP);
        s.key(0, card(1.15, 0.5, 0.5)).key(1, card(0.98, 0.5, 0.5));
        double hintAt = hint < 0 ? bar(12.5) : bar(10.5) + tenDur * hint / (ten.frames - 1.0);
        title(bar(10.5) + 0.2, hintAt - 0.15, "endless");
        title(hintAt - 0.05, bar(13.5) - 0.1, "hint");
        voice("m07", bar(10.5) + 0.3, 3.5);
        double m08At = Math.max(bar(10.5) + 4.0, hintAt - 0.2);
        double m08End = m08At + voice("m08", m08At, 2.5);

        s = shot("04-shop", bar(13.5), bar(14.5), 0, 64, Trans.WHIP);
        s.key(0, card(1.05, 0.5, 0.5)).key(1, card());
        // Titled close-ups: a big card beside the words on 16:9 (a full bleed would put the title
        // over the game's own text); full bleed under a scrim on 9:16.
        s = shot("04-shop", bar(14.5), bar(15.5), 54, 118, Trans.CUT);
        if (land) s.key(0, card(1.55, 0.5, 0.52)).key(1, card(1.7, 0.5, 0.6));
        else s.key(0, macro(z * 0.72, 0.5, 0.52, 0)).key(1, macro(z * 0.8, 0.5, 0.6, 0));
        s = shot("04-shop", bar(15.5), bar(16.5), 112, 170, Trans.CUT);
        s.key(0, card(1.32, 0.5, 0.56)).key(1, card(1.12, 0.5, 0.52));
        title(bar(13.5) + 0.2, bar(16.5) - 0.1, "styles");
        voice("m09", Math.max(bar(13.5) + 0.3, m08End + 0.2), 4);

        s = shot("05-goals", bar(16.5), bar(18.5), 0, scenes.get("05-goals").frames - 1, Trans.WHIP);
        if (land) s.key(0, card(1.6, 0.5, 0.33)).key(1, card(1.72, 0.5, 0.42));
        else s.key(0, macro(z * 0.68, 0.5, 0.33, 0)).key(1, macro(z * 0.74, 0.5, 0.42, 0));
        title(bar(16.5) + 0.2, bar(18.5) - 0.1, "star");
        voice("m10", bar(16.5) + 0.25, 2.8);

        s = shot("06-music", bar(18.5), bar(19.5), 0, scenes.get("06-music").frames - 1, Trans.WHIP);
        s.key(0, card(1.1, 0.5, 0.45)).key(1, card());
        title(bar(18.5) + 0.15, bar(19.5) - 0.05, "music");
        s = shot("07-languages", bar(19.5), bar(20.5), 0, scenes.get("07-languages").frames - 1, Trans.CUT);
        s.key(0, card()).key(1, card(1.08, 0.5, 0.45));
        title(bar(19.5) + 0.1, bar(20.5) - 0.05, "langs");
        voice("m11", bar(18.5) + 0.2, 3.6);

        // ---- Outro: logo, slogan, call to action.
        flashes.add(bar(20.5));
        logo(Kind.LOGO_OUT, bar(20.5), bar(24), Trans.CUT);
        double m12 = voice("m12", bar(20.5) + 0.45, 2.6);
        voice("m13", bar(20.5) + 0.45 + m12 + 0.45, 1.4);
        total = bar(24);

        // ---- Sound design.
        float[][] whoosh = Synth.whoosh(0.55, 11);
        float[][] impact = Synth.impact(2.2, 3);
        for (Shot sh : shots) {
            if (sh.in == Trans.WHIP) sfx(whoosh, sh.start - 0.28, 0.55, 0);
        }
        sfx(Synth.riser(bar(1.5)), bar(3.5), 0.5, 0);
        sfx(impact, bar(5), 0.95, 0);
        sfx(impact, bar(20.5), 0.95, 0);
        sfx(Synth.impact(1.2, 9), bar(9.5), 0.45, 0); // the "Perfect!" moment
        gameSounds();

        music("intro_tense_01.wav", 0, 0, bar(5), 1.5, 0.25, 0.55);
        music("main_upbeat_F_major_112.wav", bar(5), 0, bar(19), 0.02, 2.4, 0.62);
    }

    void buildShort() throws IOException {
        Scene solve = scenes.get("02-solve");
        int p0 = Math.max(0, solve.first("PICK", 0));
        int win = solve.first("WIN", p0);
        if (win < 0) win = solve.frames - 75;
        double z = macroZ();

        letterbox.add(new double[] {0, bar(1)});
        Shot s = shot("00-mess", 0, bar(1), 20, 60, Trans.DIP);
        s.key(0, macro(z * 1.1, 0.32, 0.30, 0)).key(1, macro(z * 0.95, 0.42, 0.34, 0));
        caption(0.3, voice("s01", 0.3, 1.2), "s01");

        flashes.add(bar(1));
        logo(Kind.LOGO_IN, bar(1), bar(1.5), Trans.CUT);

        s = shot("02-solve", bar(1.5), bar(3.5), p0, win, Trans.WHIP);
        s.easeOutTime = true;
        s.key(0, card(1.35, 0.5, 0.36)).key(1, card());
        title(bar(1.5) + 0.15, bar(4) - 0.1, "sort");
        voice("s02", bar(1.5) + 0.2, 2);
        s = shot("02-solve", bar(3.5), bar(4), win, win + 32, Trans.CUT);
        s.key(0, card(1.0, 0.5, 0.4)).key(1, card(1.2, 0.5, 0.32));

        s = shot("03-ten-colours", bar(4), bar(5), 20, 84, Trans.WHIP);
        s.key(0, card(1.1, 0.5, 0.5)).key(1, card());
        title(bar(4) + 0.15, bar(5) - 0.05, "endless");
        s = shot("04-shop", bar(5), bar(5.5), 115, 150, Trans.CUT);
        if (land) s.key(0, card(1.55, 0.5, 0.55)).key(1, card(1.68, 0.5, 0.58));
        else s.key(0, macro(z * 0.75, 0.5, 0.55, 0)).key(1, macro(z * 0.82, 0.5, 0.58, 0));
        title(bar(5) + 0.05, bar(5.5) - 0.05, "styles");
        voice("s03", bar(4) + 0.15, 2.4);

        flashes.add(bar(5.5));
        logo(Kind.LOGO_OUT, bar(5.5), bar(7), Trans.CUT);
        voice("s04", bar(5.5) + 0.3, 2.3);
        total = bar(7);

        float[][] whoosh = Synth.whoosh(0.5, 11);
        for (Shot sh : shots) {
            if (sh.in == Trans.WHIP) sfx(whoosh, sh.start - 0.26, 0.55, 0);
        }
        sfx(Synth.riser(bar(1)), 0, 0.45, 0);
        sfx(Synth.impact(2.0, 3), bar(1), 0.95, 0);
        sfx(Synth.impact(2.0, 3), bar(5.5), 0.95, 0);
        gameSounds();
        music("intro_tense_01.wav", 0, 0, bar(1), 0.4, 0.15, 0.5);
        music("main_upbeat_F_major_112.wav", bar(1), 0, bar(6), 0.02, 1.4, 0.62);
    }

    /** The game's own effects, placed where they happened in the filmed scenes. */
    void gameSounds() throws IOException {
        Map<String, float[][]> cache = new HashMap<>();
        for (Shot s : shots) {
            if (s.kind != Kind.CARD) continue;
            double span = s.srcEnd - s.srcStart;
            double speed = span / FPS / s.dur();
            for (Object[] e : s.scene.events) {
                int frame = (int) e[0];
                if (frame < s.srcStart || frame >= s.srcEnd) continue;
                String cue = ((String) e[1]).toLowerCase(Locale.ROOT);
                if (cue.startsWith("voice")) continue;
                if (speed > 1.8 && cue.equals("pick")) continue; // a fast montage gets the landings only
                Path file = sfxDir.resolve(cue + ".wav");
                if (!Files.exists(file)) continue;
                float[][] data = cache.computeIfAbsent(cue, c -> {
                    try {
                        return decode(file);
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                });
                double at = s.start + s.dur() * s.timeMapInverse((frame - s.srcStart) / span);
                Sound sound = new Sound();
                sound.data = data;
                sound.at = at;
                sound.gain = cue.equals("win") ? 0.9 : 0.7;
                sound.pitch = (double) e[2];
                sounds.add(sound);
            }
        }
    }

    // =================================================================== video

    void renderVideo(Path mix) throws Exception {
        int frames = (int) Math.round(total * FPS);
        List<String> cmd = new ArrayList<>(List.of(ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
            "-f", "rawvideo", "-pix_fmt", "bgr24", "-s", W + "x" + H, "-r", String.valueOf(FPS), "-i", "-",
            "-i", mix.toString(),
            "-filter_complex", "[0:v]eq=contrast=1.04:saturation=1.07:gamma=0.99,noise=alls=3:allf=t,format=yuv420p[v];"
                + "[1:a]loudnorm=I=-14:TP=-1.5:LRA=11,aresample=48000[a]",
            "-map", "[v]", "-map", "[a]", "-c:v", "libx264", "-preset", "slow", "-crf", "17", "-profile:v", "high",
            "-c:a", "aac", "-b:a", "256k", "-movflags", "+faststart", "-t", String.format(Locale.ROOT, "%.3f", total),
            out.toString()));
        Files.createDirectories(out.getParent());
        Process ff = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        int chunk = 12;
        try (OutputStream pipe = ff.getOutputStream()) {
            List<Future<byte[][]>> pending = new ArrayList<>();
            int next = 0;
            int written = 0;
            while (written < frames) {
                while (next < frames && pending.size() < threads * 2) {
                    final int from = next;
                    final int to = Math.min(frames, next + chunk);
                    pending.add(pool.submit(() -> renderChunk(from, to)));
                    next = to;
                }
                byte[][] done = pending.remove(0).get();
                for (byte[] frame : done) {
                    pipe.write(frame);
                    written++;
                }
                if (written % 150 < chunk) {
                    System.out.printf(Locale.ROOT, "[trailer]   %d / %d frames%n", written, frames);
                }
            }
        } finally {
            pool.shutdown();
        }
        if (ff.waitFor() != 0) throw new IllegalStateException("ffmpeg failed");
    }

    /** Per-thread cache of the last decoded source frame (consecutive frames share work). */
    final ThreadLocal<Object[]> lastFrame = ThreadLocal.withInitial(() -> new Object[] {null, null});

    byte[][] renderChunk(int from, int to) throws IOException {
        byte[][] result = new byte[to - from][];
        BufferedImage frame = new BufferedImage(W, H, BufferedImage.TYPE_3BYTE_BGR);
        BufferedImage layer = new BufferedImage(W, H, BufferedImage.TYPE_3BYTE_BGR);
        for (int i = from; i < to; i++) {
            double t = (i + 0.5) / FPS;
            compose(frame, layer, t);
            result[i - from] = ((DataBufferByte) frame.getRaster().getDataBuffer()).getData().clone();
        }
        return result;
    }

    void compose(BufferedImage frame, BufferedImage layer, double t) throws IOException {
        Graphics2D g = frame.createGraphics();
        hints(g);
        int index = shotAt(t);
        Shot shot = shots.get(index);
        double whip = 0.18;
        Shot next = index + 1 < shots.size() ? shots.get(index + 1) : null;
        Shot prev = index > 0 ? shots.get(index - 1) : null;

        if (next != null && next.in == Trans.WHIP && t > shot.end - whip / 2) {
            whipBetween(g, layer, shot, next, (t - (shot.end - whip / 2)) / whip, t);
        } else if (prev != null && shot.in == Trans.WHIP && t < shot.start + whip / 2) {
            whipBetween(g, layer, prev, shot, (t - (shot.start - whip / 2)) / whip, t);
        } else if (prev != null && shot.in == Trans.FADE && t < shot.start + 0.4) {
            drawShot(g, prev, Math.min(t, prev.end - 0.5 / FPS));
            Graphics2D l = layer.createGraphics();
            hints(l);
            drawShot(l, shot, t);
            l.dispose();
            g.setComposite(AlphaComposite.SrcOver.derive((float) easeInOutCubic((t - shot.start) / 0.4)));
            g.drawImage(layer, 0, 0, null);
            g.setComposite(AlphaComposite.SrcOver);
        } else {
            drawShot(g, shot, t);
        }

        drawBokeh(g, t, shot.kind == Kind.CARD ? 0.55 : 1.0);
        g.drawImage(vignette, 0, 0, null);
        drawTitles(g, t);
        drawLetterbox(g, t);
        drawCaptions(g, t);
        // Fades: in from black on DIP, out to black at the very end, white flash on impacts.
        double black = 0;
        if (shots.get(0).in == Trans.DIP) black = Math.max(black, 1 - clamp(t / 1.2, 0, 1));
        black = Math.max(black, clamp((t - (total - 0.6)) / 0.6, 0, 1));
        if (black > 0) {
            g.setColor(new Color(0, 0, 0, (int) (255 * black)));
            g.fillRect(0, 0, W, H);
        }
        double flash = 0;
        for (double f : flashes) {
            double d = t - f;
            if (d >= -1.5 / FPS && d < 0.6) flash = Math.max(flash, d < 0 ? 0.5 : 0.9 * Math.exp(-d / 0.11));
        }
        if (flash > 0.01) {
            g.setColor(new Color(255, 255, 255, (int) (255 * clamp(flash, 0, 1))));
            g.fillRect(0, 0, W, H);
        }
        g.dispose();
    }

    int shotAt(double t) {
        for (int i = 0; i < shots.size(); i++) {
            if (t < shots.get(i).end) return i;
        }
        return shots.size() - 1;
    }

    void whipBetween(Graphics2D g, BufferedImage layer, Shot a, Shot b, double p, double t) throws IOException {
        p = clamp(p, 0, 1);
        Shot showing = p < 0.5 ? a : b;
        double local = p < 0.5 ? Math.min(t, a.end - 0.5 / FPS) : Math.max(t, b.start);
        Graphics2D l = layer.createGraphics();
        hints(l);
        drawShot(l, showing, local);
        l.dispose();
        // A fast pan: the outgoing shot leaves left, the incoming arrives from the right, smeared.
        double offset = p < 0.5 ? -easeInCubic(p * 2) * W * 0.35 : (1 - easeOutCubic((p - 0.5) * 2)) * W * 0.35;
        double smear = (1 - Math.abs(2 * p - 1)) * W * 0.06;
        g.setColor(BG_BOTTOM);
        g.fillRect(0, 0, W, H);
        int copies = 5;
        for (int i = 0; i < copies; i++) {
            double dx = offset + (i - (copies - 1) / 2.0) * smear / copies * 2;
            g.setComposite(AlphaComposite.SrcOver.derive(i == 0 ? 1f : 1f / (i + 1)));
            g.drawImage(layer, (int) Math.round(dx), 0, null);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    void drawShot(Graphics2D g, Shot s, double t) throws IOException {
        double local = t - s.start;
        if (s.kind != Kind.CARD) {
            drawBackground(g, t);
            drawLogo(g, s, local);
            return;
        }
        Cam cam = s.cam(clamp(local / s.dur(), 0, 1));
        BufferedImage src = sourceFrame(s.scene, s.sourceFrame(local));
        double cardH = cam.k * H;
        double cardW = cardH * SRC_ASPECT;
        double left = cam.ax * W - cam.u * cardW;
        double top = cam.ay * H - cam.v * cardH;
        boolean covers = cam.k >= kCover * 0.999;
        if (covers) { // never show an edge on a full-bleed shot
            left = clamp(left, W - cardW, 0);
            top = clamp(top, H - cardH, 0);
        } else {
            drawBackground(g, t);
        }
        AffineTransform saved = g.getTransform();
        if (cam.rot != 0) g.rotate(Math.toRadians(cam.rot), cam.ax * W, cam.ay * H);
        double radius = covers ? 0 : cardW * 0.055 * clamp((kCover - cam.k) / (kCover * 0.25), 0, 1);
        if (!covers) {
            for (int i = 6; i >= 1; i--) { // soft shadow
                double spread = i * 9;
                g.setColor(new Color(0, 0, 0, 16));
                g.fill(new RoundRectangle2D.Double(left - spread, top - spread * 0.6 + 18, cardW + spread * 2, cardH + spread * 2,
                    radius * 2 + spread * 2, radius * 2 + spread * 2));
            }
        }
        Shape clip = new RoundRectangle2D.Double(left, top, cardW, cardH, radius * 2, radius * 2);
        Shape oldClip = g.getClip();
        if (!covers) g.clip(clip);
        drawScaled(g, src, left, top, cardW, cardH);
        g.setClip(oldClip);
        if (!covers) {
            g.setStroke(new BasicStroke(2.5f));
            g.setColor(new Color(255, 255, 255, 70));
            g.draw(clip);
        }
        g.setTransform(saved);
    }

    /** Draws with a pre-halved source when shrinking a lot, so text in the footage stays clean. */
    void drawScaled(Graphics2D g, BufferedImage src, double x, double y, double w, double h) {
        BufferedImage img = src;
        while (w < img.getWidth() * 0.5) {
            BufferedImage half = new BufferedImage(img.getWidth() / 2, img.getHeight() / 2, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D hg = half.createGraphics();
            hg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            hg.drawImage(img, 0, 0, half.getWidth(), half.getHeight(), null);
            hg.dispose();
            img = half;
        }
        AffineTransform at = new AffineTransform();
        at.translate(x, y);
        at.scale(w / img.getWidth(), h / img.getHeight());
        g.drawImage(img, at, null);
    }

    BufferedImage sourceFrame(Scene scene, int index) throws IOException {
        Object[] cache = lastFrame.get();
        String key = scene.name + "#" + index;
        if (key.equals(cache[0])) return (BufferedImage) cache[1];
        BufferedImage raw = ImageIO.read(scene.dir.resolve(String.format(Locale.ROOT, "%05d.png", index)).toFile());
        BufferedImage img = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = img.createGraphics();
        g.drawImage(raw, 0, 0, null);
        g.dispose();
        cache[0] = key;
        cache[1] = img;
        return img;
    }

    void drawBackground(Graphics2D g, double t) {
        g.drawImage(background, 0, 0, null);
        // Two slow aurora glows in the game's colours.
        double s = Math.max(W, H);
        drawGlow(g, glow[0], W * (0.25 + 0.08 * Math.sin(t * 0.35)), H * (0.3 + 0.06 * Math.cos(t * 0.27)), s * 0.9, 0.22);
        drawGlow(g, glow[1], W * (0.78 + 0.07 * Math.cos(t * 0.31)), H * (0.72 + 0.05 * Math.sin(t * 0.23)), s * 0.95, 0.26);
    }

    static void drawGlow(Graphics2D g, BufferedImage img, double cx, double cy, double size, double alpha) {
        g.setComposite(AlphaComposite.SrcOver.derive((float) clamp(alpha, 0, 1)));
        g.drawImage(img, (int) (cx - size / 2), (int) (cy - size / 2), (int) size, (int) size, null);
        g.setComposite(AlphaComposite.SrcOver);
    }

    /** Floating light motes, the same every render (fixed seed). */
    void drawBokeh(Graphics2D g, double t, double strength) {
        Random r = new Random(42);
        int n = land ? 34 : 30;
        for (int i = 0; i < n; i++) {
            double x0 = r.nextDouble(), y0 = r.nextDouble();
            double size = 10 + Math.pow(r.nextDouble(), 2.2) * 120;
            double vx = (r.nextDouble() - 0.5) * 0.012, vy = -0.006 - r.nextDouble() * 0.014;
            double phase = r.nextDouble() * 6.28;
            double x = frac(x0 + vx * t) * (W + 200) - 100;
            double y = frac(y0 + vy * t) * (H + 200) - 100;
            double a = (0.05 + 0.13 * (0.5 + 0.5 * Math.sin(t * 0.9 + phase))) * strength * (size < 40 ? 1.6 : 0.8);
            drawGlow(g, glow[2], x, y, size, a);
        }
    }

    // ------------------------------------------------------------------ logo

    void drawLogo(Graphics2D g, Shot s, double local) {
        boolean outro = s.kind == Kind.LOGO_OUT;
        double iconSize = land ? (outro ? 250 : 300) : (outro ? 330 : 380);
        double cx = W / 2.0;
        double cy = land ? (outro ? H * 0.30 : H * 0.36) : (outro ? H * 0.29 : H * 0.36);
        double slam = 0.2;
        double scale;
        if (local < slam) {
            scale = 1.7 - 0.75 * easeInCubic(local / slam);
        } else {
            double d = local - slam;
            scale = 1 - 0.05 * Math.exp(-d * 7) * Math.cos(d * 22);
        }
        scale *= 1 + 0.012 * Math.sin(local * 2.0);
        double alpha = clamp(local / 0.08, 0, 1);

        // Halo behind the icon, then the particle burst from the impact.
        drawGlow(g, glow[0], cx, cy, iconSize * 3.2, 0.35 * alpha);
        drawBurst(g, cx, cy, local - slam);

        double size = iconSize * scale;
        g.setComposite(AlphaComposite.SrcOver.derive((float) alpha));
        g.drawImage(icon, (int) (cx - size / 2), (int) (cy - size / 2), (int) size, (int) size, null);
        g.setComposite(AlphaComposite.SrcOver);
        // Light sweep across the icon.
        double sweep = (local - 0.55) / 0.7;
        if (sweep > 0 && sweep < 1) {
            Shape old = g.getClip();
            g.clip(new RoundRectangle2D.Double(cx - size / 2, cy - size / 2, size, size, size * 0.43, size * 0.43));
            double bx = cx - size + sweep * size * 2;
            g.setPaint(new GradientPaint((float) (bx - 60), 0, new Color(255, 255, 255, 0), (float) bx, 0, new Color(255, 255, 255, 120), true));
            g.fill(new Rectangle2D.Double(bx - 60, cy - size, 120, size * 2));
            g.setClip(old);
        }

        double wordSize = land ? (outro ? 120 : 140) : (outro ? 128 : 140);
        double wordY = cy + iconSize * 0.5 + wordSize * 1.05;
        drawReveal(g, "Tidy Tubes", titleFont, wordSize, cx, wordY, true, local - 0.3, s.dur() - local, TEXT, false, true);
        String slogan = copy.get("slogan");
        double sloganSize = land ? 44 : 50;
        double sloganY = wordY + sloganSize * 1.7;
        if (outro || !"short".equals(cut)) {
            drawReveal(g, slogan, fontFor(slogan, false), sloganSize, cx, sloganY, true, local - (outro ? 0.6 : 0.65), s.dur() - local, PRIMARY, false, false);
        }
        if (outro) {
            drawCta(g, copy.get("cta"), cx, sloganY + (land ? 120 : 150), local - (s.dur() > 2 ? 1.5 : 0.8));
        }
    }

    void drawBurst(Graphics2D g, double cx, double cy, double t) {
        if (t < 0 || t > 1.6) return;
        Random r = new Random(7);
        for (int i = 0; i < 64; i++) {
            double angle = r.nextDouble() * Math.PI * 2;
            double speed = 500 + r.nextDouble() * 900;
            double size = 10 + r.nextDouble() * 22;
            Color c = BALLS[r.nextInt(BALLS.length)];
            double dist = speed * (1 - Math.exp(-t * 3.2)) / 3.2;
            double x = cx + Math.cos(angle) * dist;
            double y = cy + Math.sin(angle) * dist + 60 * t * t;
            double a = clamp(1 - t / 1.6, 0, 1);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (235 * a)));
            double d = size * (1 - 0.4 * t / 1.6);
            g.fill(new java.awt.geom.Ellipse2D.Double(x - d / 2, y - d / 2, d, d));
        }
    }

    void drawCta(Graphics2D g, String text, double cx, double cy, double t) {
        if (t < 0) return;
        double e = easeOutBack(clamp(t / 0.45, 0, 1));
        double pulse = 1 + 0.03 * Math.sin(Math.max(0, t - 0.6) * 5.5);
        double size = (land ? 40 : 46);
        Font f = fontFor(text, true).deriveFont((float) size);
        TextLayout layout = layout(text, f, g.getFontRenderContext(), false);
        double tw = layout.getAdvance();
        double ph = size * 2.1, pw = tw + size * 2.2;
        AffineTransform saved = g.getTransform();
        g.translate(cx, cy);
        g.scale(e * pulse, e * pulse);
        g.setComposite(AlphaComposite.SrcOver.derive((float) clamp(t / 0.2, 0, 1)));
        drawGlow(g, glow[0], 0, 0, pw * 1.6, 0.35);
        g.setColor(PRIMARY);
        g.fill(new RoundRectangle2D.Double(-pw / 2, -ph / 2, pw, ph, ph, ph));
        g.setColor(ON_PRIMARY);
        layout.draw(g, (float) (-tw / 2), (float) (layout.getAscent() / 2 - layout.getDescent() / 2));
        g.setComposite(AlphaComposite.SrcOver);
        g.setTransform(saved);
    }

    // ------------------------------------------------------------------ text

    boolean arabic() {
        return "ar".equals(lang);
    }

    Font fontFor(String text, boolean bold) {
        boolean hasArabic = text.codePoints().anyMatch(c -> c >= 0x0600 && c <= 0x06FF);
        if (hasArabic) return bold ? arabicTitle : arabicLine;
        return bold ? titleFont : lineFont;
    }

    /** Text layout with Latin fallback inside Arabic lines (digits, "Google Play"). */
    TextLayout layout(String text, Font font, FontRenderContext frc, boolean tracking) {
        AttributedString as = new AttributedString(text);
        as.addAttribute(TextAttribute.FONT, font);
        if (text.codePoints().anyMatch(c -> c >= 0x0600 && c <= 0x06FF)) {
            // Right-to-left paragraph: Latin words ("Google Play") and punctuation sit where an
            // Arabic reader expects them.
            as.addAttribute(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL);
        }
        if (font == arabicTitle || font == arabicLine || font.getFontName().contains("Arabic")) {
            Font latin = (font.getFontName().contains("Bold") ? titleFont : lineFont).deriveFont(font.getSize2D() * 0.92f);
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < 0x0600 && !Character.isWhitespace(c) && "،؟.!:".indexOf(c) < 0) {
                    as.addAttribute(TextAttribute.FONT, latin, i, i + 1);
                }
            }
        } else if (tracking) {
            as.addAttribute(TextAttribute.TRACKING, 0.03f);
        }
        return new TextLayout(as.getIterator(), frc);
    }

    /**
     * Mask reveal: the line rises out of an invisible slot, holds, then fades up and out.
     * Works the same for Arabic (shaped by Java's TextLayout) as for Latin text.
     */
    void drawReveal(Graphics2D g, String text, Font base, double size, double x, double baseline, boolean center,
                    double t, double remaining, Color color, boolean upper, boolean glowing) {
        if (text == null || text.isEmpty() || t < 0) return;
        boolean latin = !text.codePoints().anyMatch(c -> c >= 0x0600 && c <= 0x06FF);
        String shown = upper && latin ? text.toUpperCase(Locale.ROOT) : text;
        Font font = base.deriveFont((float) size);
        TextLayout layout = layout(shown, font, g.getFontRenderContext(), upper);
        double width = layout.getAdvance();
        double maxWidth = land ? (center ? W * 0.86 : W * 0.5) : W * 0.9;
        if (width > maxWidth) {
            font = base.deriveFont((float) (size * maxWidth / width));
            layout = layout(shown, font, g.getFontRenderContext(), upper);
            width = layout.getAdvance();
        }
        double ascent = layout.getAscent(), descent = layout.getDescent();
        double left = center ? x - width / 2 : x;
        double in = easeOutExpo(clamp(t / 0.55, 0, 1));
        double out = clamp((0.3 - remaining) / 0.3, 0, 1);
        double dy = (1 - in) * (ascent + descent) - out * 18;
        float alpha = (float) (clamp(t / 0.25, 0, 1) * (1 - out));
        Shape oldClip = g.getClip();
        g.clip(new Rectangle2D.Double(left - 40, baseline - ascent - 30, width + 80, ascent + descent + 40));
        g.setComposite(AlphaComposite.SrcOver.derive(alpha * 0.45f));
        g.setColor(Color.BLACK);
        layout.draw(g, (float) left, (float) (baseline + dy + 5));
        if (glowing) {
            g.setComposite(AlphaComposite.SrcOver.derive(alpha * 0.25f));
            g.setColor(PRIMARY);
            for (int i = 0; i < 4; i++) {
                layout.draw(g, (float) (left + Math.cos(i * 1.57) * 3), (float) (baseline + dy + Math.sin(i * 1.57) * 3));
            }
        }
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g.setColor(color);
        layout.draw(g, (float) left, (float) (baseline + dy));
        // A light sweep across big titles as they land.
        if (glowing || size > 70) {
            double sweep = (t - 0.35) / 0.6;
            if (sweep > 0 && sweep < 1) {
                Shape outline = layout.getOutline(AffineTransform.getTranslateInstance(left, baseline + dy));
                double bx = left - 80 + sweep * (width + 160);
                g.setPaint(new GradientPaint((float) (bx - 50), 0, new Color(255, 255, 255, 0), (float) bx, 0,
                    new Color(255, 255, 255, 170), true));
                g.fill(outline);
            }
        }
        g.setComposite(AlphaComposite.SrcOver);
        g.setClip(oldClip);
    }

    void drawTitles(Graphics2D g, double t) {
        for (Title title : titles) {
            if (t < title.start || t > title.end) continue;
            double local = t - title.start, remaining = title.end - t;
            // Scrim behind the words, so they read over any footage.
            double scrim = 0.62 * clamp(local / 0.3, 0, 1) * clamp(remaining / 0.3, 0, 1);
            if (land) {
                g.setPaint(new GradientPaint(0, 0, new Color(5, 7, 21, (int) (255 * scrim)), (float) (W * 0.62), 0, new Color(5, 7, 21, 0)));
                g.fillRect(0, 0, (int) (W * 0.62), H);
            } else {
                g.setPaint(new GradientPaint(0, 0, new Color(5, 7, 21, (int) (255 * scrim)), 0, (float) (H * 0.2), new Color(5, 7, 21, 0)));
                g.fillRect(0, 0, W, (int) (H * 0.2));
            }
            if (land) {
                double cx = W * 0.315;
                drawReveal(g, title.title, fontFor(title.title, true), 92, cx, H * 0.47, true, local, remaining, TEXT, true, false);
                drawReveal(g, title.line, fontFor(title.line, false), 40, cx, H * 0.47 + 78, true, local - 0.14, remaining, PRIMARY, false, false);
            } else {
                drawReveal(g, title.title, fontFor(title.title, true), 84, W / 2.0, H * 0.085, true, local, remaining, TEXT, true, false);
                drawReveal(g, title.line, fontFor(title.line, false), 38, W / 2.0, H * 0.085 + 66, true, local - 0.14, remaining, PRIMARY, false, false);
            }
        }
    }

    double letterboxAmount(double t) {
        double amount = 0;
        for (double[] lb : letterbox) {
            if (t >= lb[0] && t < lb[1]) amount = 1;
            else if (t >= lb[1] && t < lb[1] + 0.5) amount = Math.max(amount, 1 - easeInOutCubic((t - lb[1]) / 0.5));
        }
        return amount;
    }

    void drawLetterbox(Graphics2D g, double t) {
        double amount = letterboxAmount(t);
        if (amount <= 0) return;
        double bar = land ? (H - W / 2.39) / 2 : H * 0.07;
        int h = (int) Math.round(bar * amount);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, h);
        g.fillRect(0, H - h, W, h);
    }

    void drawCaptions(Graphics2D g, double t) {
        for (Caption c : captions) {
            if (t < c.start || t > c.end + 0.35 || c.text == null) continue;
            double a = clamp((t - c.start) / 0.2, 0, 1) * clamp((c.end + 0.35 - t) / 0.3, 0, 1);
            float size = land ? 42 : 46;
            Font f = fontFor(c.text, false).deriveFont(size);
            TextLayout layout = layout(c.text, f, g.getFontRenderContext(), false);
            double w = layout.getAdvance();
            if (w > W * 0.9) { // long translations shrink to fit instead of running off the frame
                f = f.deriveFont((float) (size * W * 0.9 / w));
                layout = layout(c.text, f, g.getFontRenderContext(), false);
                w = layout.getAdvance();
            }
            double y = land ? H - (H - W / 2.39) / 4 + layout.getAscent() / 3 : H * 0.84;
            g.setComposite(AlphaComposite.SrcOver.derive((float) (a * 0.6)));
            g.setColor(Color.BLACK);
            layout.draw(g, (float) (W / 2.0 - w / 2), (float) y + 3);
            g.setComposite(AlphaComposite.SrcOver.derive((float) a));
            g.setColor(new Color(244, 246, 255));
            layout.draw(g, (float) (W / 2.0 - w / 2), (float) y);
            g.setComposite(AlphaComposite.SrcOver);
        }
    }

    // =================================================================== audio

    float[][] decode(Path file) throws IOException {
        Process p = new ProcessBuilder(ffmpeg, "-hide_banner", "-loglevel", "error", "-i", file.toString(),
            "-f", "f32le", "-ac", "2", "-ar", String.valueOf(RATE), "-").start();
        byte[] bytes;
        try (InputStream in = p.getInputStream()) {
            bytes = in.readAllBytes();
        }
        try {
            if (p.waitFor() != 0) throw new IOException("ffmpeg could not decode " + file);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int n = bytes.length / 8;
        float[][] data = new float[2][n];
        for (int i = 0; i < n; i++) {
            data[0][i] = buf.getFloat();
            data[1][i] = buf.getFloat();
        }
        return data;
    }

    void mixAudio(Path wav) throws IOException {
        int n = (int) (total * RATE) + RATE;
        float[][] bed = new float[2][n];
        float[][] fx = new float[2][n];
        float[][] vo = new float[2][n];
        for (MusicCue m : music) {
            int start = (int) (m.at * RATE);
            int len = (int) (m.length * RATE);
            int src = m.data[0].length;
            for (int i = 0; i < len && start + i < n; i++) {
                int j = ((int) (m.from * RATE) + i) % src; // loops if the bed is shorter
                double tt = (double) i / RATE;
                double env = Math.min(1, tt / Math.max(1e-3, m.fadeIn)) * Math.min(1, (m.length - tt) / Math.max(1e-3, m.fadeOut));
                env = clamp(env, 0, 1) * m.gain;
                bed[0][start + i] += (float) (m.data[0][j] * env);
                bed[1][start + i] += (float) (m.data[1][j] * env);
            }
        }
        for (Sound s : sounds) {
            float[][] target = s.voice ? vo : fx;
            int start = (int) Math.round(s.at * RATE);
            int len = (int) (s.data[0].length / s.pitch);
            double l = s.gain * Math.min(1, 1 - s.pan), r = s.gain * Math.min(1, 1 + s.pan);
            for (int i = 0; i < len; i++) {
                int k = start + i;
                if (k < 0 || k >= n) continue;
                double pos = i * s.pitch;
                int j = (int) pos;
                if (j + 1 >= s.data[0].length) break;
                double f = pos - j;
                target[0][k] += (float) ((s.data[0][j] * (1 - f) + s.data[0][j + 1] * f) * l);
                target[1][k] += (float) ((s.data[1][j] * (1 - f) + s.data[1][j + 1] * f) * r);
            }
        }
        // Ducking: the music dips ~8 dB under the narration, with smooth attack and release.
        double env = 0, attack = Math.exp(-1.0 / (0.06 * RATE)), release = Math.exp(-1.0 / (0.45 * RATE));
        float[][] master = new float[2][n];
        double peak = 0;
        for (int i = 0; i < n; i++) {
            double level = Math.max(Math.abs(vo[0][i]), Math.abs(vo[1][i])) > 0.015 ? 1 : 0;
            env = level > env ? level + (env - level) * attack : level + (env - level) * release;
            double duck = 1 - 0.6 * env;
            for (int c = 0; c < 2; c++) {
                double v = bed[c][i] * duck + fx[c][i] * 0.8 + vo[c][i] * 1.0;
                master[c][i] = (float) v;
                peak = Math.max(peak, Math.abs(v));
            }
        }
        // Soft limiter above -3 dBFS; loudnorm in FFmpeg sets the final level.
        double gain = peak > 0.98 ? 0.98 / peak : 1;
        try (DataOutputStream out = new DataOutputStream(new java.io.BufferedOutputStream(new FileOutputStream(wav.toFile())))) {
            writeWavHeader(out, n);
            ByteBuffer b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < n; i++) {
                for (int c = 0; c < 2; c++) {
                    double v = master[c][i] * gain;
                    v = Math.abs(v) < 0.7 ? v : Math.signum(v) * (0.7 + 0.3 * Math.tanh((Math.abs(v) - 0.7) / 0.3));
                    short s = (short) Math.round(clamp(v, -1, 1) * 32767);
                    b.clear();
                    b.putShort(s);
                    out.write(b.array(), 0, 2);
                }
            }
        }
    }

    static void writeWavHeader(DataOutputStream out, int frames) throws IOException {
        int data = frames * 4;
        ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + data).put("WAVE".getBytes(StandardCharsets.US_ASCII));
        h.put("fmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16).putShort((short) 1).putShort((short) 2).putInt(RATE)
            .putInt(RATE * 4).putShort((short) 4).putShort((short) 16);
        h.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(data);
        out.write(h.array());
    }

    /** Original trailer sound design, synthesised (no samples, no licences). */
    static final class Synth {

        /** Air whoosh: band-passed noise sweeping up then down, panned left to right. */
        static float[][] whoosh(double seconds, long seed) {
            int n = (int) (seconds * RATE);
            float[][] o = new float[2][n];
            Random r = new Random(seed);
            double low = 0, band = 0;
            for (int i = 0; i < n; i++) {
                double x = (double) i / n;
                double env = Math.pow(Math.sin(Math.PI * Math.pow(x, 0.8)), 2);
                double cutoff = 250 + 3200 * Math.sin(Math.PI * x);
                double f = 2 * Math.sin(Math.PI * cutoff / RATE);
                double noise = r.nextGaussian() * 0.5;
                low += f * band;
                double high = noise - low - 0.7 * band;
                band += f * high;
                double v = band * env * 0.55;
                double pan = -0.7 + 1.4 * x;
                o[0][i] = (float) (v * (1 - Math.max(0, pan)));
                o[1][i] = (float) (v * (1 + Math.min(0, pan)));
            }
            return o;
        }

        /** Cinematic hit: pitched sub drop, a short body thump and a dark tail. */
        static float[][] impact(double seconds, long seed) {
            int n = (int) (seconds * RATE);
            float[][] o = new float[2][n];
            Random r = new Random(seed);
            double phase = 0, low = 0, tailL = 0, tailR = 0;
            for (int i = 0; i < n; i++) {
                double t = (double) i / RATE;
                double freq = 38 + 52 * Math.exp(-t * 9);
                phase += 2 * Math.PI * freq / RATE;
                double sub = Math.sin(phase) * Math.exp(-t * 2.6) * 0.95;
                double noise = r.nextGaussian();
                low += 0.08 * (noise - low);
                double body = low * Math.exp(-t * 22) * 1.6;
                tailL += 0.02 * (r.nextGaussian() - tailL);
                tailR += 0.02 * (r.nextGaussian() - tailR);
                double tail = Math.exp(-t * 2.2) * 0.9;
                double attack = Math.min(1, t / 0.002);
                o[0][i] = (float) ((sub + body + tailL * tail) * attack * 0.8);
                o[1][i] = (float) ((sub + body + tailR * tail) * attack * 0.8);
            }
            return o;
        }

        /** Tension riser: noise opening up plus a soft rising tone, cut dead at the drop. */
        static float[][] riser(double seconds) {
            int n = (int) (seconds * RATE);
            float[][] o = new float[2][n];
            Random r = new Random(5);
            double low = 0, band = 0, phase = 0, phase2 = 0;
            for (int i = 0; i < n; i++) {
                double x = (double) i / n;
                double cutoff = 300 + 5200 * x * x;
                double f = 2 * Math.sin(Math.PI * Math.min(cutoff, RATE / 6.0) / RATE);
                double noise = r.nextGaussian() * 0.4;
                low += f * band;
                double high = noise - low - 0.9 * band;
                band += f * high;
                double tone = 174.6 * Math.pow(2, x * 1.5); // F3 up an octave and a half
                phase += 2 * Math.PI * tone / RATE;
                phase2 += 2 * Math.PI * tone * 1.5 / RATE;
                double env = Math.pow(x, 1.8);
                double v = (band * 0.5 + Math.sin(phase) * 0.12 + Math.sin(phase2) * 0.06) * env;
                double wobble = Math.sin(i * 2 * Math.PI * 0.35 / RATE * (1 + 6 * x));
                o[0][i] = (float) (v * (1 + 0.25 * wobble));
                o[1][i] = (float) (v * (1 - 0.25 * wobble));
            }
            return o;
        }
    }

    // =================================================================== maths

    static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    static double frac(double v) {
        return v - Math.floor(v);
    }

    static double easeInOutCubic(double t) {
        t = clamp(t, 0, 1);
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    static double easeInCubic(double t) {
        t = clamp(t, 0, 1);
        return t * t * t;
    }

    static double easeOutCubic(double t) {
        t = clamp(t, 0, 1);
        return 1 - Math.pow(1 - t, 3);
    }

    static double easeOutExpo(double t) {
        t = clamp(t, 0, 1);
        return t >= 1 ? 1 : 1 - Math.pow(2, -10 * t);
    }

    static double easeOutBack(double t) {
        t = clamp(t, 0, 1);
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    // =================================================================== words

    /** On-screen words per language. Titles are short; narration captions match the voice. */
    static final class Copy {
        static Map<String, String> of(String lang) {
            Map<String, String> m = new HashMap<>();
            switch (lang) {
                case "fr" -> {
                    put(m, "sort", "Trie les couleurs", "Une couleur par tube");
                    put(m, "endless", "Niveaux à l'infini", "Jusqu'à 10 couleurs. Tous ont une solution.");
                    put(m, "hint", "Bloqué ? Un indice", "Calculé par un vrai solveur");
                    put(m, "styles", "400 styles", "Billes, tubes, finitions et thèmes");
                    put(m, "star", "Route des étoiles", "Chaque étoile débloque des styles exclusifs");
                    put(m, "music", "Musique relaxante", "10 boucles apaisantes");
                    put(m, "langs", "8 langues", "Joue hors ligne, quand tu veux");
                    m.put("slogan", "Trie. Respire. Recommence.");
                    m.put("cta", "GRATUIT SUR GOOGLE PLAY");
                    m.put("m01", "Quelque part, les couleurs sont toutes mélangées.");
                    m.put("m02", "Emmêlées. Agitées. Pas à leur place.");
                    m.put("m03", "Il suffit d'un geste...");
                    m.put("m04", "pour remettre de l'ordre.");
                    m.put("s01", "Des couleurs en désordre ?");
                }
                case "ar" -> {
                    put(m, "sort", "رتّب الألوان", "لون واحد في كل أنبوب");
                    put(m, "endless", "مستويات لا تنتهي", "حتى 10 ألوان، وكل مستوى له حل");
                    put(m, "hint", "عالق؟ خذ تلميحًا", "من حلّال حقيقي");
                    put(m, "styles", "400 مظهر", "كرات وأنابيب ولمسات وسمات");
                    put(m, "star", "طريق النجوم", "كل نجمة تفتح مظاهر حصرية");
                    put(m, "music", "موسيقى مريحة", "10 مقطوعات هادئة");
                    put(m, "langs", "8 لغات", "العب دون إنترنت، في أي وقت");
                    m.put("slogan", "رتّب. تنفّس. كرّر.");
                    m.put("cta", "مجانًا على Google Play");
                    m.put("m01", "في مكانٍ ما، اختلطت الألوان كلها.");
                    m.put("m02", "متشابكة. قلقة. في غير مكانها.");
                    m.put("m03", "تكفي حركة واحدة...");
                    m.put("m04", "لتعيد كل شيء إلى مكانه.");
                    m.put("s01", "ألوان في غير مكانها؟");
                }
                default -> {
                    put(m, "sort", "Sort the colours", "One colour per tube");
                    put(m, "endless", "Endless levels", "Up to 10 colours. Every one solvable.");
                    put(m, "hint", "Stuck? Get a hint", "From a real solver");
                    put(m, "styles", "400 styles", "Balls, tubes, finishes and themes");
                    put(m, "star", "Star road", "Every star unlocks exclusive skins");
                    put(m, "music", "Relaxing music", "10 calm loops");
                    put(m, "langs", "8 languages", "Play offline, anytime");
                    m.put("slogan", "Sort. Breathe. Repeat.");
                    m.put("cta", "FREE ON GOOGLE PLAY");
                    m.put("m01", "Somewhere, the colours are all mixed up.");
                    m.put("m02", "Tangled. Restless. Out of place.");
                    m.put("m03", "It only takes one move...");
                    m.put("m04", "to set things right.");
                    m.put("s01", "Colours out of place?");
                }
            }
            return m;
        }

        static void put(Map<String, String> m, String key, String title, String line) {
            m.put(key + ".t", title);
            m.put(key + ".l", line);
        }
    }
}
