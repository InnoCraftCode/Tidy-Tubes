package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;

import com.ben.laama.sortpuzzle.logic.Breathing;

/**
 * Thirty-six seconds of guided breathing over the board: a scrim, one soft circle that
 * swells on the inhale, rests, and settles on the exhale, and three words. A tap anywhere
 * ends it early. Timing comes from {@link Breathing}; nothing here is allocated per frame.
 */
public final class BreatheOverlay extends Group {

    /** Receives whether the breath was completed (true) or skipped by a tap (false). */
    public interface Listener {
        void onFinished(boolean completed);
    }

    private static final float MIN_RADIUS = 70f;
    private static final float MAX_RADIUS = 190f;

    private final Drawable scrim;
    private final Drawable glow;
    private final Drawable disc;
    private final Label phase;
    private final Label cycle;
    private final Label hint;
    private final Listener listener;
    private final Color tint = new Color();
    private float seconds;
    private boolean finished;
    private Breathing.Phase lastPhase;

    public BreatheOverlay(Skin skin, float width, float height, Listener listener) {
        this.listener = listener;
        scrim = skin.newDrawable("white", Palette.SCRIM);
        glow = skin.getDrawable("glow");
        disc = skin.getDrawable("circle");
        setSize(width, height);
        setTouchable(Touchable.enabled);

        phase = new Label("", skin, "title");
        phase.setAlignment(Align.center);
        cycle = new Label("", skin, "dim");
        cycle.setAlignment(Align.center);
        hint = new Label("Tap to skip", skin, "caption");
        hint.setAlignment(Align.center);
        hint.getColor().a = 0.7f;
        addActor(phase);
        addActor(cycle);
        addActor(hint);
        layout();

        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                finish(false);
            }
        });
        getColor().a = 0f;
        addAction(Actions.fadeIn(0.4f, Interpolation.fade));
    }

    private void layout() {
        float cx = getWidth() * 0.5f;
        float cy = getHeight() * 0.54f;
        phase.setPosition(cx, cy + MAX_RADIUS + 70f, Align.center);
        cycle.setPosition(cx, cy - MAX_RADIUS - 56f, Align.center);
        hint.setPosition(cx, 140f, Align.center);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (finished) return;
        seconds += delta;
        Breathing.Phase now = Breathing.phaseAt(seconds);
        if (now == Breathing.Phase.DONE) {
            finish(true);
            return;
        }
        if (now != lastPhase) {
            lastPhase = now;
            phase.setText(now.label());
            cycle.setText(Breathing.cycleAt(seconds) + " / " + Breathing.CYCLES);
            phase.clearActions();
            phase.getColor().a = 0.2f;
            phase.addAction(Actions.fadeIn(0.5f, Interpolation.fade));
        }
    }

    private void finish(boolean completed) {
        if (finished) return;
        finished = true;
        setTouchable(Touchable.disabled);
        addAction(Actions.sequence(
            Actions.fadeOut(0.35f, Interpolation.fade),
            Actions.run(() -> listener.onFinished(completed)),
            Actions.removeActor()));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float alpha = getColor().a * parentAlpha;
        tint.set(Color.WHITE);
        tint.a = alpha;
        batch.setColor(tint);
        scrim.draw(batch, getX(), getY(), getWidth(), getHeight());

        float fullness = Breathing.fullnessAt(seconds);
        float radius = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * fullness;
        float cx = getX() + getWidth() * 0.5f;
        float cy = getY() + getHeight() * 0.54f;

        tint.set(Palette.PRIMARY);
        tint.a = (0.18f + 0.22f * fullness) * alpha;
        batch.setColor(tint);
        float glowSize = radius * 3.2f;
        glow.draw(batch, cx - glowSize * 0.5f, cy - glowSize * 0.5f, glowSize, glowSize);

        tint.set(Palette.PRIMARY);
        tint.a = (0.55f + 0.3f * fullness) * alpha;
        batch.setColor(tint);
        disc.draw(batch, cx - radius, cy - radius, radius * 2f, radius * 2f);

        batch.setColor(Color.WHITE);
        super.draw(batch, parentAlpha);
    }
}
