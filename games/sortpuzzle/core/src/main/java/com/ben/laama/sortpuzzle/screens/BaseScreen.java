package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;



import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.logic.Cosmetic;
import com.ben.laama.sortpuzzle.ui.AnimatedBackground;
import com.ben.laama.sortpuzzle.ui.Cosmetics;
import com.ben.laama.sortpuzzle.ui.Responsive;
import com.ben.laama.sortpuzzle.ui.SafeInsets;

/**
 * Shared plumbing for every screen: a 720x1280 {@link ExtendViewport} stage (it grows to fill
 * taller or wider screens instead of letterboxing them) sharing the game-wide
 * {@link com.badlogic.gdx.graphics.g2d.SpriteBatch}, the animated backdrop, a {@link #safeArea}
 * that keeps the UI clear of display cutouts, a fade-in, press feedback on every button, and
 * ESC / Android-back handling.
 */
public abstract class BaseScreen extends ScreenAdapter {

    protected final Main game;
    protected final Skin skin;
    protected final Stage stage;
    /** Gradient plus drifting balls. Screens with a busy foreground turn its intensity down. */
    protected final AnimatedBackground background;
    /**
     * The stage minus the display-cutout insets. Fill-parent UI goes here, so a camera hole or
     * a rounded corner never covers a button, while the backdrop still paints edge to edge.
     */
    protected final Group safeArea;
    /**
     * True when the screen is at least as wide as it is tall (phone on its side, tablet,
     * unfolded foldable). The viewport then uses a 1280x800 minimum and screens lay out in
     * columns; see {@link Responsive}.
     */
    private boolean landscape;

    protected BaseScreen(Main game) {
        this.game = game;
        this.skin = game.skin();
        // Constructors lay out for the current orientation; resize() switches it afterwards.
        this.landscape = Responsive.isLandscape(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.stage = new Stage(
            new ExtendViewport(Responsive.minWorldWidth(landscape), Responsive.minWorldHeight(landscape),
                Responsive.maxWorldWidth(landscape), Responsive.maxWorldHeight(landscape)),
            game.batch());

        background = new AnimatedBackground(skin);
        stage.addActor(background);

        safeArea = new Group();
        safeArea.setTouchable(Touchable.childrenOnly); // never swallow taps meant for the board
        stage.addActor(safeArea);

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

    /**
     * Fades the whole screen out (ease-in, about two thirds of the entrance time) and then runs
     * {@code then}. Input is cut at once, so a double tap cannot queue a second navigation; any
     * pending root actions (delayed celebrations and the like) are dropped with the screen.
     * {@code then} runs inside {@code stage.act}, so it must defer anything that disposes this
     * stage (see {@link Main#switchTo}).
     */
    public void leave(Runnable then) {
        Group root = stage.getRoot();
        root.setTouchable(Touchable.disabled);
        root.clearActions();
        root.addAction(Actions.sequence(
            Actions.fadeOut(0.16f, Interpolation.pow2In),
            Actions.run(then)));
    }

    /**
     * The theme this screen wears instead of the equipped one (a campaign level wears its
     * chapter's biome), or {@code null} for the equipped look.
     */
    protected Cosmetic themeOverride() {
        return null;
    }

    /**
     * Puts the renderer in this screen's look: the equipped cosmetics, then
     * {@link #themeOverride()}. Runs in {@link #show()}, i.e. after the previous screen's
     * {@code hide()}, so no other screen can change the theme under this one afterwards.
     * Screens never restore the look in {@code hide()}: the next screen sets its own.
     */
    protected void applyLook() {
        Cosmetics.apply(skin, game.tubes(), game.save());
        Cosmetic override = themeOverride();
        if (override != null) {
            Cosmetics.applyItem(skin, game.tubes(), override);
        }
        background.refresh(skin);
    }

    @Override
    public void show() {
        applyLook();
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
        ExtendViewport viewport = (ExtendViewport) stage.getViewport();
        landscape = Responsive.isLandscape(width, height);
        viewport.setMinWorldWidth(Responsive.minWorldWidth(landscape));
        viewport.setMinWorldHeight(Responsive.minWorldHeight(landscape));
        viewport.setMaxWorldWidth(Responsive.maxWorldWidth(landscape));
        viewport.setMaxWorldHeight(Responsive.maxWorldHeight(landscape));
        viewport.update(width, height, true);
        background.setBounds(0f, 0f, stage.getWidth(), stage.getHeight());
        SafeInsets insets = SafeInsets.toWorld(
            Gdx.graphics.getSafeInsetTop(), Gdx.graphics.getSafeInsetBottom(),
            Gdx.graphics.getSafeInsetLeft(), Gdx.graphics.getSafeInsetRight(),
            width, height, viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight(), stage.getWidth());
        safeArea.setBounds(insets.left, insets.bottom,
            stage.getWidth() - insets.left - insets.right,
            stage.getHeight() - insets.top - insets.bottom);
        onResized(stage.getWidth(), stage.getHeight());
    }

    /** @return true while the screen is in its landscape (column) layout */
    protected boolean isLandscape() {
        return landscape;
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
