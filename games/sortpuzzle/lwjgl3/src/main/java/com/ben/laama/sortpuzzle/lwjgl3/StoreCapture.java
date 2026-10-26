package com.ben.laama.sortpuzzle.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiMode;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.screens.GameScreen;
import com.ben.laama.sortpuzzle.screens.HomeScreen;
import com.ben.laama.sortpuzzle.ui.SkinFactory;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;
import java.util.zip.Deflater;

/**
 * Films the game for Google Play: store screenshots and the frames of a promo video.
 *
 * <p>The game renders off-screen into a 1080x1920 frame buffer (Play's recommended portrait
 * size) while a small window shows a preview. A scripted timeline drives it through the same
 * public entry points the UI uses - {@code goToLevel}, the hint button, and
 * {@link GameScreen#autoPlay} for real, solver-played moves - so every animation is genuine.</p>
 *
 * <ul>
 *   <li>{@code screens}: writes the listing screenshots, real-time.</li>
 *   <li>{@code video}: writes every frame at a fixed 30 fps (deterministic, slower than real
 *       time), to be encoded by {@code tools\store\make-promo-video.ps1}.</li>
 * </ul>
 * Preferences go to a throwaway folder, so the developer's own desktop save is never touched,
 * and sound is muted during capture.
 *
 * <pre>gradlew :lwjgl3:storeCapture -PcaptureMode=screens|video</pre>
 */
public final class StoreCapture extends ApplicationAdapter {

    /** Play's recommended phone size; tablets pass their own (e.g. 1200x1920, 1600x2560). */
    private static final int DEFAULT_WIDTH = 1080;
    private static final int DEFAULT_HEIGHT = 1920;
    private static final float VIDEO_STEP = 1f / 30f;

    private final int WIDTH;
    private final int HEIGHT;
    private final boolean video;
    private final File output;
    private final Deque<Step> timeline = new ArrayDeque<>();

    private Main game;
    private FrameBuffer frame;
    private SpriteBatch preview;
    private Screen sizedScreen;
    private float stepTime;
    private boolean recording;
    private String pendingShot;
    private int frameNumber;

    private StoreCapture(boolean video, File output, int width, int height) {
        this.video = video;
        this.output = output;
        this.WIDTH = width;
        this.HEIGHT = height;
    }

    /**
     * Arguments: {@code screens|video} {@code <output dir>} {@code <prefs dir>} [{@code WxH}].
     * The size is the frame-buffer (screenshot) size; the window shows a half-size preview.
     */
    public static void main(String[] args) {
        boolean video = args.length > 0 && args[0].equals("video");
        File output = new File(args.length > 1 ? args[1] : "capture");
        File prefs = new File(args.length > 2 ? args[2] : "capture-prefs");
        int width = DEFAULT_WIDTH;
        int height = DEFAULT_HEIGHT;
        if (args.length > 3 && args[3].matches("\\d+x\\d+")) {
            width = Integer.parseInt(args[3].substring(0, args[3].indexOf('x')));
            height = Integer.parseInt(args[3].substring(args[3].indexOf('x') + 1));
        }
        deleteRecursively(prefs);
        if (video) {
            deleteRecursively(output);
        }
        output.mkdirs();

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(GameConfig.APP_NAME + " - store capture (" + (video ? "video" : "screenshots")
            + " " + width + "x" + height + ")");
        config.setWindowedMode(width / 2, height / 2);
        config.setResizable(false);
        config.setHdpiMode(HdpiMode.Pixels); // keep glViewport maths in real pixels inside the FBO
        config.useVsync(!video);
        config.setForegroundFPS(video ? 0 : 60);
        config.setPreferencesConfig(prefs.getAbsolutePath(), Files.FileType.Absolute);
        // Silent capture without touching the mute flag (the home screen shows sound as on).
        config.disableAudio(true);
        new Lwjgl3Application(new StoreCapture(video, output, width, height), config);
    }

    // ------------------------------------------------------------------ scripts

    private void screenshotScript() {
        waitFor(() -> game.getScreen() instanceof HomeScreen, 10f);
        wait(1.4f);
        shot("01-home");

        level(24);
        wait(1.6f);
        run(() -> gameScreen().autoPlay(0.7f, 2));
        wait(1.7f);
        shot("02-level-in-progress");

        run(() -> gameScreen().onHint());
        wait(1.0f);
        shot("03-hint");

        level(4);
        wait(1.2f);
        run(() -> gameScreen().autoPlay(0.5f, 99));
        waitFor(() -> gameScreen().isWon(), 40f);
        wait(0.95f);
        shot("04-level-cleared");

        run(() -> game.goLevelSelect());
        wait(1.2f);
        shot("05-levels");

        level(105);
        wait(1.6f);
        run(() -> gameScreen().autoPlay(0.7f, 3));
        wait(3.8f); // let any praise word fade: this shot is about the full 10-colour board
        shot("06-ten-colours");

        run(() -> game.goSettings());
        wait(1.2f);
        shot("07-settings");
    }

    /** About 30 seconds: home, a full solve with celebration, a hint, the 10-colour endgame. */
    private void videoScript() {
        run(() -> recording = true);
        waitFor(() -> game.getScreen() instanceof HomeScreen, 10f);
        wait(1.6f);

        level(6);
        wait(1.0f);
        run(() -> gameScreen().autoPlay(0.62f, 99));
        waitFor(() -> gameScreen().isWon(), 40f);
        wait(2.4f);

        level(28);
        wait(1.0f);
        run(() -> gameScreen().autoPlay(0.6f, 3));
        wait(2.0f);
        run(() -> gameScreen().onHint());
        wait(2.2f);

        level(104);
        wait(1.2f);
        run(() -> gameScreen().autoPlay(0.6f, 4));
        wait(2.8f);

        run(() -> game.goHome());
        wait(1.6f);
        run(() -> recording = false);
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public void create() {
        // Fonts must be rasterised for the frame buffer, not the half-size preview. The world
        // scale is the smaller of the two ratios (ExtendViewport fits the 720x1280 minimum).
        SkinFactory.setDensityOverride(Math.min(WIDTH / GameConfig.WORLD_WIDTH,
            HEIGHT / GameConfig.WORLD_HEIGHT));
        game = new Main();
        game.create();
        // Look like a player some way in: the level grid shows progress, the HUD has coins,
        // and the first-launch tutorial card stays out of the pictures.
        for (int level = 1; level <= 36; level++) {
            game.save().recordWin(level, 14, 12);
        }
        game.save().markTutorialSeen();
        frame = new FrameBuffer(Pixmap.Format.RGBA8888, WIDTH, HEIGHT, false);
        preview = new SpriteBatch();
        if (video) {
            videoScript();
        } else {
            screenshotScript();
        }
    }

    @Override
    public void render() {
        float delta = video ? VIDEO_STEP : Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        Screen screen = game.getScreen();
        if (screen != sizedScreen && screen != null) {
            // Game.setScreen sizes new screens to the preview window; re-size to the capture.
            screen.resize(WIDTH, HEIGHT);
            sizedScreen = screen;
        }

        frame.begin();
        Gdx.gl.glViewport(0, 0, WIDTH, HEIGHT);
        if (screen != null) {
            screen.render(delta);
        }
        if (pendingShot != null) {
            save(new File(output, pendingShot + ".png"));
            System.out.println("[capture] " + pendingShot + ".png");
            pendingShot = null;
        }
        if (recording) {
            save(new File(output, String.format("%05d.png", frameNumber++)));
        }
        frame.end();

        drawPreview();
        advance(delta);
    }

    private void drawPreview() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        preview.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        preview.begin();
        // Frame-buffer textures are upside down: draw with v flipped.
        preview.draw(frame.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
            0, 0, WIDTH, HEIGHT, false, true);
        preview.end();
    }

    private void save(File file) {
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, WIDTH, HEIGHT);
        PixmapIO.writePNG(Gdx.files.absolute(file.getAbsolutePath()), pixels, Deflater.BEST_SPEED, true);
        pixels.dispose();
    }

    @Override
    public void dispose() {
        if (frame != null) frame.dispose();
        if (preview != null) preview.dispose();
        if (game != null) game.dispose();
        if (video) {
            System.out.println("[capture] " + frameNumber + " frames (" + frameNumber / 30f + " s) in " + output);
        }
    }

    // ----------------------------------------------------------------- timeline

    private interface Step {
        /** @return true when finished; {@code elapsed} is the time since this step began */
        boolean tick(float elapsed);
    }

    private void advance(float delta) {
        stepTime += delta;
        while (!timeline.isEmpty() && timeline.peek().tick(stepTime)) {
            timeline.poll();
            stepTime = 0f;
        }
        if (timeline.isEmpty() && pendingShot == null) {
            Gdx.app.exit();
        }
    }

    private void wait(float seconds) {
        timeline.add(elapsed -> elapsed >= seconds);
    }

    private void run(Runnable action) {
        timeline.add(elapsed -> {
            action.run();
            return true;
        });
    }

    private void waitFor(BooleanSupplier condition, float timeout) {
        timeline.add(elapsed -> {
            if (elapsed > timeout) {
                throw new IllegalStateException("store capture timed out waiting " + timeout + " s");
            }
            return condition.getAsBoolean();
        });
    }

    private void shot(String name) {
        timeline.add(elapsed -> {
            pendingShot = name; // written on the next rendered frame
            return true;
        });
        wait(VIDEO_STEP * 2);
    }

    /** Opens a level and waits until the board is dealt and playable. */
    private void level(int index) {
        run(() -> game.goToLevel(index));
        waitFor(() -> game.getScreen() instanceof GameScreen g && g.levelIndex() == index && g.isReady(), 30f);
    }

    private GameScreen gameScreen() {
        return (GameScreen) game.getScreen();
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
