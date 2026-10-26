package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.ui.AnimatedBackground;

/**
 * Shared plumbing for every screen: a 720x1280 {@link ExtendViewport} stage (it grows to fill
 * taller or wider screens instead of letterboxing them) sharing the game-wide
 * {@link com.badlogic.gdx.graphics.g2d.SpriteBatch}, the animated backdrop, a fade-in, press
 * feedback on every button, and ESC / Android-back handling.
 */
public abstract class BaseScreen extends ScreenAdapter {

    protected final Main game;
    protected final Skin skin;
    protected final Stage stage;
    /** Gradient plus drifting balls. Screens with a busy foreground turn its intensity down. */
    protected final AnimatedBackground background;

    protected BaseScreen(Main game) {
        this.game = game;
        this.skin = game.skin();
        this.stage = new Stage(
            new ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT,
                GameConfig.WORLD_MAX_WIDTH, GameConfig.WORLD_MAX_HEIGHT), game.batch());

        background = new AnimatedBackground(skin);
        stage.addActor(background);

        // Every button on every screen squashes a little when pressed and springs back.
        stage.getRoot().addCaptureListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                Button pressed = event.getTarget().firstAscendant(Button.class);
                if (pressed != null && !pressed.isDisabled()) {
                    pressed.setTransform(true);
                    pressed.setOrigin(Align.center);
                    pressed.clearActions();
                    pressed.addAction(Actions.sequence(
                        Actions.scaleTo(0.92f, 0.92f, 0.06f, Interpolation.sineOut),
                        Actions.scaleTo(1f, 1f, 0.22f, Interpolation.swingOut)));
                }
                return false; // observe only; the button still gets its click
            }
        });

        stage.getRoot().addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    onBackPressed();
                    return true;
                }
                return false;
            }
        });
    }

    /** Called on ESC (desktop) or the hardware back button (Android). */
    protected void onBackPressed() {
        game.goHome();
    }

    @Override
    public void show() {
        // Every screen fades in, so switching screens never pops.
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.25f, Interpolation.fade));
        Gdx.input.setInputProcessor(stage);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        stage.setKeyboardFocus(stage.getRoot());
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        // Clamp so a stalled frame never teleports animations.
        stage.act(Math.min(delta, 1f / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        background.setBounds(0f, 0f, stage.getWidth(), stage.getHeight());
        onResized(stage.getWidth(), stage.getHeight());
    }

    /** Hook for screens that place actors by hand rather than through fill-parent tables. */
    protected void onResized(float worldWidth, float worldHeight) {
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
