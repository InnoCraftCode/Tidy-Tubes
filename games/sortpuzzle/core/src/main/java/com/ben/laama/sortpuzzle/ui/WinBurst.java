package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Disposable;

/**
 * The little firework that goes off when a level is finished.
 *
 * <p>Drawn with {@link ShapeRenderer} rather than sprites, which means interrupting the stage's
 * batch for one pass — the actor ends the batch, draws, and starts it again, so it must be the
 * last child of the stage.</p>
 *
 * <p>Particle state lives in parallel {@code float[]} arrays allocated once. Nothing here
 * allocates per frame, and when the burst is over {@link #act} returns immediately.</p>
 */
public final class WinBurst extends Actor implements Disposable {

    private static final int CAPACITY = 160;
    private static final int CIRCLE_SEGMENTS = 10;
    private static final float GRAVITY = -900f;
    private static final float DRAG = 0.86f;

    private final ShapeRenderer shapes = new ShapeRenderer();

    private final float[] x = new float[CAPACITY];
    private final float[] y = new float[CAPACITY];
    private final float[] velocityX = new float[CAPACITY];
    private final float[] velocityY = new float[CAPACITY];
    private final float[] age = new float[CAPACITY];
    private final float[] lifespan = new float[CAPACITY];
    private final float[] radius = new float[CAPACITY];
    private final int[] colorIndex = new int[CAPACITY];

    private int live;

    public WinBurst() {
        setTouchable(Touchable.disabled);
    }

    /**
     * Launches a burst.
     *
     * @param centerX    stage x to launch from
     * @param centerY    stage y to launch from
     * @param colorCount how many of the palette's colours to draw from
     */
    public void fire(float centerX, float centerY, int colorCount) {
        live = CAPACITY;
        for (int i = 0; i < CAPACITY; i++) {
            float angle = MathUtils.random(MathUtils.PI2);
            float speed = MathUtils.random(180f, 760f);
            x[i] = centerX + MathUtils.random(-18f, 18f);
            y[i] = centerY + MathUtils.random(-18f, 18f);
            velocityX[i] = MathUtils.cos(angle) * speed;
            velocityY[i] = MathUtils.sin(angle) * speed + MathUtils.random(120f, 420f);
            age[i] = 0f;
            lifespan[i] = MathUtils.random(0.65f, 1.35f);
            radius[i] = MathUtils.random(3.5f, 9f);
            colorIndex[i] = MathUtils.random(Math.max(1, colorCount) - 1);
        }
    }

    /**
     * A small, single-colour fountain (a tube was just completed). Reuses only particles that
     * have already died, so it never cuts short a burst that is still in the air.
     *
     * @param count      particles to launch at most
     * @param colorIndex palette colour of the completed tube
     */
    public void sparkle(float centerX, float centerY, int colorIndex, int count) {
        int launched = 0;
        for (int i = 0; i < CAPACITY && launched < count; i++) {
            if (age[i] < lifespan[i]) {
                continue;
            }
            float angle = MathUtils.random(MathUtils.PI * 0.15f, MathUtils.PI * 0.85f); // upwards
            float speed = MathUtils.random(160f, 460f);
            x[i] = centerX + MathUtils.random(-14f, 14f);
            y[i] = centerY;
            velocityX[i] = MathUtils.cos(angle) * speed;
            velocityY[i] = MathUtils.sin(angle) * speed;
            age[i] = 0f;
            lifespan[i] = MathUtils.random(0.45f, 0.9f);
            radius[i] = MathUtils.random(3f, 7f);
            this.colorIndex[i] = colorIndex;
            launched++;
        }
        live += launched;
    }

    public boolean isFinished() {
        return live == 0;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (live == 0) {
            return;
        }
        int remaining = 0;
        for (int i = 0; i < CAPACITY; i++) {
            if (age[i] >= lifespan[i]) {
                continue;
            }
            age[i] += delta;
            velocityY[i] += GRAVITY * delta;
            // DRAG is expressed per 60fps frame; scale it by delta so the burst looks the same
            // on a 120Hz phone as on a 60Hz one.
            float damping = MathUtils.clamp(1f - (1f - DRAG) * delta * 60f, 0f, 1f);
            velocityX[i] *= damping;
            x[i] += velocityX[i] * delta;
            y[i] += velocityY[i] * delta;
            if (age[i] < lifespan[i]) {
                remaining++;
            }
        }
        live = remaining;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (live == 0) {
            return;
        }
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(batch.getProjectionMatrix());
        shapes.setTransformMatrix(batch.getTransformMatrix());
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < CAPACITY; i++) {
            if (age[i] >= lifespan[i]) {
                continue;
            }
            float remaining = 1f - age[i] / lifespan[i];
            Color color = Palette.ball(colorIndex[i]);
            shapes.setColor(color.r, color.g, color.b, remaining * parentAlpha);
            shapes.circle(x[i], y[i], radius[i] * remaining, CIRCLE_SEGMENTS);
        }

        shapes.end();
        batch.begin();
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
