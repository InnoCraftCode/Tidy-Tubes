package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.model.Packed;

/**
 * The glass tube. Draws itself (soft shadow, shaded body, rim) and owns the geometry every
 * ball needs: where each slot sits, and how high above the mouth a lifted ball floats.
 *
 * <p>Balls are deliberately <em>not</em> children of this actor — they travel between tubes, so
 * they live in a single group drawn on top and are positioned in stage coordinates using the
 * helpers here.</p>
 */
public final class TubeActor extends Actor {

    private static final Color TINT = new Color();
    /** Space between the glass and the bottom ball; also the ball's side clearance. */
    public static final float INNER_PADDING = 9f;

    private final Drawable shadow;
    private final int index;
    private final float ballSize;
    private final TubeTextures textures;

    private Drawable body;
    private Drawable rim;
    private boolean highlighted;
    private boolean suggested;
    private float pulse;
    private float homeX;
    private float homeY;

    /**
     * @param index    position of this tube in the {@link com.ben.laama.sortpuzzle.model.BoardState}
     * @param ballSize diameter of the balls this tube holds
     */
    public TubeActor(Skin skin, TubeTextures textures, int index, float ballSize) {
        this.shadow = skin.getDrawable("glow");
        this.textures = textures;
        this.index = index;
        this.ballSize = ballSize;
        setTouchable(Touchable.enabled);
    }

    public int index() {
        return index;
    }

    @Override
    protected void sizeChanged() {
        body = textures.body(Math.round(getWidth()), Math.round(getHeight()));
        rim = textures.rim(Math.round(getWidth()), Math.round(getHeight()));
    }

    /**
     * Places the tube and records where it belongs.
     *
     * <p>Deliberately not an override of {@code setPosition}: the shake animation drives that
     * method every frame, and an override would quietly redefine "home" as wherever the wobble
     * happens to be, leaving the tube — and every ball slot computed from it — a few pixels off
     * when the animation ends.</p>
     */
    public void setHome(float x, float y) {
        setPosition(x, y);
        homeX = x;
        homeY = y;
    }

    // ---------------------------------------------------------------- geometry
    /** @return stage x of a ball sitting in this tube, bottom-left corner */
    public float ballX() {
        return homeX + (getWidth() - ballSize) * 0.5f;
    }

    /**
     * @param slot 0 = bottom
     * @return stage y of that slot, bottom-left corner
     */
    public float ballY(int slot) {
        return homeY + INNER_PADDING + slot * ballSize;
    }

    /** @return stage y of a ball lifted out of the tube, ready to travel */
    public float liftY() {
        return homeY + getHeight() + GameConfig.BALL_LIFT_HEIGHT - ballSize;
    }

    public float ballSize() {
        return ballSize;
    }

    // ------------------------------------------------------------------ visuals
    public void setHighlighted(boolean value) {
        highlighted = value;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    /**
     * Marks the tube as part of the move the hint system is proposing. Drawn as a pulsing gold
     * rim — deliberately a different colour from the selection highlight, so "you picked this"
     * and "the game is pointing here" never look alike.
     */
    public void setSuggested(boolean value) {
        suggested = value;
        if (!value) {
            pulse = 0f;
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (suggested) {
            pulse += delta;
        }
    }

    /** Quick left-right wobble that always ends exactly back home. */
    public void shake() {
        clearActions();
        setPosition(homeX, homeY);
        float amplitude = GameConfig.SHAKE_AMPLITUDE;
        float slice = GameConfig.SHAKE_DURATION / 6f;
        addAction(Actions.sequence(
            Actions.moveBy(amplitude, 0f, slice, Interpolation.sineOut),
            Actions.moveBy(-amplitude * 2f, 0f, slice * 2f, Interpolation.sine),
            Actions.moveBy(amplitude * 1.6f, 0f, slice, Interpolation.sine),
            Actions.moveBy(-amplitude * 0.6f, 0f, slice, Interpolation.sine),
            Actions.run(() -> setPosition(homeX, homeY))));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (body == null) {
            return;
        }
        float alpha = getColor().a * parentAlpha;
        float w = getWidth();
        float h = getHeight();

        // Contact shadow: a squashed glow under the tube's base.
        TINT.set(Palette.SHADOW);
        TINT.a *= alpha;
        batch.setColor(TINT);
        shadow.draw(batch, getX() - w * 0.35f, getY() - w * 0.42f, w * 1.7f, w * 0.7f);

        TINT.set(highlighted ? Palette.SURFACE_STRONG : Palette.SURFACE);
        TINT.a *= alpha;
        batch.setColor(TINT);
        body.draw(batch, getX(), getY(), w, h);

        if (suggested) {
            TINT.set(Palette.GOLD);
            TINT.a *= alpha * (0.55f + 0.45f * MathUtils.sin(pulse * 6f));
        } else {
            TINT.set(highlighted ? Palette.PRIMARY : Palette.EDGE);
            TINT.a *= alpha * (highlighted ? 1f : 0.7f);
        }
        batch.setColor(TINT);
        rim.draw(batch, getX(), getY(), w, h);

        batch.setColor(Color.WHITE);
    }

    /** @return the height a tube needs to hold a full stack of {@code ballSize} balls */
    public static float heightFor(float ballSize) {
        return ballSize * Packed.CAPACITY + INNER_PADDING * 2f;
    }

    /** @return the tube width that leaves {@link #INNER_PADDING} either side of the ball */
    public static float widthFor(float ballSize) {
        return ballSize + INNER_PADDING * 2f;
    }
}
