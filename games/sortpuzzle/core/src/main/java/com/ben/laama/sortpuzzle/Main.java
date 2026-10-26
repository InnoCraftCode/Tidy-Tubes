package com.ben.laama.sortpuzzle;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import com.ben.laama.sortpuzzle.ads.AdPacing;
import com.ben.laama.sortpuzzle.ads.AdProvider;
import com.ben.laama.sortpuzzle.ads.AdProviders;
import com.ben.laama.sortpuzzle.audio.SoundManager;
import com.ben.laama.sortpuzzle.save.SaveData;
import com.ben.laama.sortpuzzle.screens.GameScreen;
import com.ben.laama.sortpuzzle.screens.HomeScreen;
import com.ben.laama.sortpuzzle.screens.LevelSelectScreen;
import com.ben.laama.sortpuzzle.screens.SettingsScreen;
import com.ben.laama.sortpuzzle.screens.SplashScreen;
import com.ben.laama.sortpuzzle.ui.SkinFactory;
import com.ben.laama.sortpuzzle.ui.TubeTextures;

/**
 * Application entry point and screen router.
 *
 * <p>Owns the things every screen needs — the shared {@link SpriteBatch}, the procedurally
 * built {@link Skin}, the tube texture cache and the player's {@link SaveData} — plus the audio
 * and advertising services.</p>
 */
public final class Main extends Game {

    private final AdProvider ads;
    private final AdPacing pacing = new AdPacing(GameConfig.INTERSTITIAL_EVERY_N_LEVELS,
        GameConfig.INTERSTITIAL_MIN_INTERVAL_MS, System::currentTimeMillis);

    private SpriteBatch batch;
    private Skin skin;
    private TubeTextures tubes;
    private SaveData save;
    private SoundManager sound;

    /** Desktop / test entry point: the provider follows {@code AdsConfig}. */
    public Main() {
        this(AdProviders.forDesktop());
    }

    /** Platform entry point: the launcher injects the provider it got from {@code AdProviders}. */
    public Main(AdProvider ads) {
        this.ads = ads == null ? AdProviders.forDesktop() : ads;
    }

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_INFO);
        batch = new SpriteBatch();
        skin = SkinFactory.create();
        tubes = new TubeTextures();
        save = SaveData.load();
        sound = new SoundManager(save);
        sound.startMusic();
        ads.initialize();
        setScreen(new SplashScreen(this));
    }

    // ------------------------------------------------------------- services
    public SpriteBatch batch() {
        return batch;
    }

    public Skin skin() {
        return skin;
    }

    public TubeTextures tubes() {
        return tubes;
    }

    public SaveData save() {
        return save;
    }

    public SoundManager sound() {
        return sound;
    }

    public AdProvider ads() {
        return ads;
    }

    /** Session-wide interstitial frequency caps. */
    public AdPacing pacing() {
        return pacing;
    }

    // ----------------------------------------------------------- navigation
    public void goHome() {
        switchTo(new HomeScreen(this));
    }

    public void goLevelSelect() {
        switchTo(new LevelSelectScreen(this));
    }

    public void goSettings() {
        switchTo(new SettingsScreen(this));
    }

    public void goToLevel(int levelIndex) {
        switchTo(new GameScreen(this, levelIndex));
    }

    /**
     * Replaces the active screen and disposes the old one.
     *
     * <p>Deferred to the start of the next frame so it is always safe to call from inside a
     * scene2d listener — the outgoing stage is never disposed mid-dispatch.</p>
     */
    public void switchTo(final Screen next) {
        Gdx.app.postRunnable(() -> {
            Screen previous = getScreen();
            setScreen(next);
            if (previous != null) {
                previous.dispose();
            }
        });
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        if (current != null) {
            current.dispose();
        }
        setScreen(null);
        if (sound != null) sound.dispose();
        if (tubes != null) tubes.dispose();
        if (skin != null) skin.dispose();
        if (batch != null) batch.dispose();
        ads.dispose();
        super.dispose();
    }
}
