package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * A soft ring that expands and fades where a ball lands: the "contact" the squash alone does
 * not sell. One drawable, one tint, removed by itself after a third of a second.
 */
public final class Ripple extends Actor {

    private static final float DURATION = 0.32f;

    private final Drawable glow;
    private final Color tint = new Color();
    private final float maxRadius;
    private float time;

    private Ripple(Skin skin, float centerX, float centerY, float maxRadius, Color color) {
        this.glow = skin.getDrawable("glow");
        this.maxRadius = maxRadius;
        tint.set(color);
        setPosition(centerX, centerY);
        setTouchable(Touchable.disabled);
    }

    /** Adds a ripple to {@code parent}, centred on the point, in the ball's colour. */
    public static void at(Skin skin, com.badlogic.gdx.scenes.scene2d.Group parent, float centerX,
                          float centerY, float ballSize, Color color) {
        parent.addActor(new Ripple(skin, centerX, centerY, ballSize * 1.4f, color));
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
        if (time >= DURATION) {
            remove();
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float t = Math.min(1f, time / DURATION);
        float radius = maxRadius * Interpolation.pow2Out.apply(t);
        float alpha = (1f - t) * 0.45f * parentAlpha;
        batch.setColor(tint.r, tint.g, tint.b, alpha);
        glow.draw(batch, getX() - radius, getY() - radius * 0.55f, radius * 2f, radius * 1.1f);
        batch.setColor(Color.WHITE);
    }
}
