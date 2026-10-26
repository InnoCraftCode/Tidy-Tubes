package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import com.ben.laama.sortpuzzle.GameConfig;

/**
 * The living backdrop: the gradient, two slow colour washes ("aurora"), and a drift of
 * translucent balls rising through it.
 *
 * <p>Balls rather than abstract shapes because this is a game about balls — the background is
 * the same object the player is sorting, just far away and out of focus. They rise, sway, and
 * wrap around when they leave the top.</p>
 *
 * <p>State lives in parallel {@code float[]} arrays allocated once, so nothing is created per
 * frame. The whole thing is a handful of textured quads and costs nothing worth measuring.</p>
 *
 * <p>{@link #setIntensity} exists because the same backdrop cannot serve both jobs: on the menus
 * it should be noticed, behind the board it must not compete with the tubes for attention.</p>
 */
public final class AnimatedBackground extends Actor {

    private static final int BALL_COUNT = 14;
    private static final float MIN_RADIUS = 26f;
    private static final float MAX_RADIUS = 96f;
    private static final float MIN_RISE = 8f;
    private static final float MAX_RISE = 30f;
    private static final float BASE_ALPHA = 0.12f;
    private static final float AURORA_ALPHA = 0.20f;

    private final Drawable gradient;
    private final Drawable ball;
    private final Drawable glow;
    private final Color tint = new Color();

    private final float[] x = new float[BALL_COUNT];
    private final float[] y = new float[BALL_COUNT];
    private final float[] radius = new float[BALL_COUNT];
    private final float[] rise = new float[BALL_COUNT];
    private final float[] swayPhase = new float[BALL_COUNT];
    private final float[] swaySpeed = new float[BALL_COUNT];
    private final float[] swayAmount = new float[BALL_COUNT];
    private final float[] alpha = new float[BALL_COUNT];
    private final int[] colorIndex = new int[BALL_COUNT];

    private float intensity = 1f;
    private float time;

    public AnimatedBackground(Skin skin) {
        this.gradient = skin.getDrawable("bg");
        this.ball = skin.getDrawable("ball");
        this.glow = skin.getDrawable("glow");
        setTouchable(Touchable.disabled);
        setBounds(0f, 0f, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);

        for (int i = 0; i < BALL_COUNT; i++) {
            spawn(i);
            // Start the field already scattered up the screen instead of all at the bottom.
            y[i] = MathUtils.random(-radius[i], getHeight());
        }
    }

    private void spawn(int i) {
        radius[i] = MathUtils.random(MIN_RADIUS, MAX_RADIUS);
        x[i] = MathUtils.random(-radius[i], getWidth() + radius[i]);
        y[i] = -radius[i] * 2f;
        rise[i] = MathUtils.random(MIN_RISE, MAX_RISE);
        swayPhase[i] = MathUtils.random(MathUtils.PI2);
        swaySpeed[i] = MathUtils.random(0.15f, 0.5f);
        swayAmount[i] = MathUtils.random(10f, 42f);
        // Bigger means further away, so fainter.
        float depth = 1f - (radius[i] - MIN_RADIUS) / (MAX_RADIUS - MIN_RADIUS);
        alpha[i] = BASE_ALPHA * (0.45f + 0.55f * depth);
        colorIndex[i] = MathUtils.random(Palette.ballCount() - 1);
    }

    /**
     * @param value 1 for the menus, something closer to 0.3 behind the board where the backdrop
     *              must stay out of the way
     */
    public void setIntensity(float value) {
        intensity = MathUtils.clamp(value, 0f, 1f);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
        for (int i = 0; i < BALL_COUNT; i++) {
            y[i] += rise[i] * delta;
            swayPhase[i] += swaySpeed[i] * delta;
            if (y[i] - radius[i] > getHeight()) {
                spawn(i);
            }
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float alphaScale = getColor().a * parentAlpha;
        float w = getWidth();
        float h = getHeight();

        batch.setColor(1f, 1f, 1f, alphaScale);
        gradient.draw(batch, getX(), getY(), w, h);

        // Aurora: two huge, very soft washes that breathe slowly.
        float breathe = 0.5f + 0.5f * MathUtils.sin(time * 0.25f);
        aurora(batch, Palette.AURORA_A, w * (-0.15f + 0.08f * breathe), h * 0.62f, w * 1.2f,
            (0.8f + 0.2f * breathe) * alphaScale);
        aurora(batch, Palette.AURORA_B, w * (0.45f - 0.06f * breathe), h * 0.05f, w * 1.1f,
            (0.7f + 0.3f * (1f - breathe)) * alphaScale);

        if (intensity > 0f) {
            for (int i = 0; i < BALL_COUNT; i++) {
                Color color = Palette.ball(colorIndex[i]);
                tint.set(color);
                tint.a = alpha[i] * intensity * alphaScale;
                batch.setColor(tint);

                float drift = MathUtils.sin(swayPhase[i]) * swayAmount[i];
                float size = radius[i] * 2f;
                ball.draw(batch, x[i] + drift - radius[i], y[i] - radius[i], size, size);
            }
        }

        batch.setColor(Color.WHITE);
    }

    private void aurora(Batch batch, Color color, float x, float y, float size, float alpha) {
        tint.set(color);
        tint.a = AURORA_ALPHA * alpha * (0.5f + 0.5f * intensity);
        batch.setColor(tint);
        glow.draw(batch, getX() + x, getY() + y, size, size);
    }
}
