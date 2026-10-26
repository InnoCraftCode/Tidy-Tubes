package com.example.gametemplate;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import com.example.gametemplate.ads.AdPacing;
import com.example.gametemplate.ads.AdProvider;
import com.example.gametemplate.ads.NoOpAdProvider;
import com.example.gametemplate.screens.PlayScreen;

/** Entry point. Owns the shared render resources, persistence and the ad service. */
public final class TemplateGame extends Game {

    private final AdProvider ads;
    private AdPacing adPacing;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private Preferences prefs;

    /** Desktop / test entry point: no ads. */
    public TemplateGame() {
        this(new NoOpAdProvider());
    }

    /** Platform entry point: the launcher injects its ad implementation. */
    public TemplateGame(AdProvider ads) {
        this.ads = ads == null ? new NoOpAdProvider() : ads;
    }

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_INFO);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont(); // 15px Arial bundled in gdx.jar; scaled per use
        font.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
        prefs = Gdx.app.getPreferences("gametemplate");
        adPacing = new AdPacing(GameConfig.INTERSTITIAL_EVERY_N_ROUNDS,
                GameConfig.INTERSTITIAL_MIN_INTERVAL_MS, System::currentTimeMillis);
        ads.initialize();
        setScreen(new PlayScreen(this));
    }

    public SpriteBatch batch() {
        return batch;
    }

    public ShapeRenderer shapes() {
        return shapes;
    }

    public BitmapFont font() {
        return font;
    }

    public AdProvider ads() {
        return ads;
    }

    public AdPacing adPacing() {
        return adPacing;
    }

    public int bestScore() {
        return prefs.getInteger("best", 0);
    }

    public void submitScore(int score) {
        if (score > bestScore()) {
            prefs.putInteger("best", score);
            prefs.flush();
        }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        if (font != null) font.dispose();
        if (shapes != null) shapes.dispose();
        if (batch != null) batch.dispose();
        ads.dispose();
        super.dispose();
    }
}
