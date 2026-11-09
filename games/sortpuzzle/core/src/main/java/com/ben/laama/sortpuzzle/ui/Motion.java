package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;

/**
 * The menus' shared motion vocabulary, so every screen moves with the same timing:
 * entrances 260 ms ease-out with a 45 ms stagger, pops 80 + 160 ms with a soft overshoot,
 * a slow "breathing" loop for the one action that invites a tap.
 *
 * <p>Everything returns exactly to rest (alpha 1, scale 1) and never touches positions, so a
 * Table relayout during an animation cannot leave an actor offset. Scale needs a transform,
 * which costs a batch flush: entrances switch it on only for their own duration.</p>
 *
 * <p>{@link #reduced} follows the system "remove animations" setting (set by the Android
 * launcher): when on, entrances and loops are skipped and actors simply appear.</p>
 */
public final class Motion {

    /** Android "Remove animations" / animator scale 0. Read once at start-up. */
    public static volatile boolean reduced = false;

    public static final float ENTER = 0.26f;
    public static final float STAGGER = 0.045f;
    /** Entrances wait one frame so tables have a size before the origin is taken. */
    private static final float FIRST_FRAME = 0.02f;
    private static final float ENTER_SCALE = 0.95f;

    private Motion() {
    }

    /**
     * Staggered entrance: fades in from {@link #ENTER_SCALE} to rest, {@code index} slots after
     * the first. Safe on Tables (no position change).
     */
    public static void enter(Actor actor, int index) {
        if (reduced) {
            return;
        }
        final boolean wasTransform = actor instanceof Group && ((Group) actor).isTransform();
        actor.getColor().a = 0f;
        actor.addAction(Actions.sequence(
            Actions.delay(FIRST_FRAME + STAGGER * index),
            Actions.run(() -> {
                if (actor instanceof Group) {
                    ((Group) actor).setTransform(true);
                }
                actor.setOrigin(Align.center);
                actor.setScale(ENTER_SCALE);
            }),
            Actions.parallel(
                Actions.fadeIn(ENTER, Interpolation.pow2Out),
                Actions.scaleTo(1f, 1f, ENTER, Interpolation.pow2Out)),
            Actions.run(() -> {
                if (actor instanceof Group) {
                    ((Group) actor).setTransform(wasTransform); // back to cheap batching
                }
            })));
    }

    /** Enters each actor in order, one stagger slot apart. */
    public static void cascade(Actor... actors) {
        for (int i = 0; i < actors.length; i++) {
            if (actors[i] != null) {
                enter(actors[i], i);
            }
        }
    }

    /** Small "this changed" pop (counter, badge): 80 ms up, 160 ms soft overshoot back. */
    public static void pop(Actor actor) {
        if (reduced) {
            return;
        }
        if (actor instanceof Group) {
            ((Group) actor).setTransform(true);
        }
        actor.setOrigin(Align.center);
        actor.clearActions();
        actor.addAction(Actions.sequence(
            Actions.scaleTo(1.14f, 1.14f, 0.08f, Interpolation.pow2Out),
            Actions.scaleTo(1f, 1f, 0.16f, Interpolation.swingOut)));
    }

    /**
     * A slow, barely visible 2 % pulse for the main call to action. The press feedback clears
     * it on touch, which is fine: a press leaves the screen.
     */
    public static void breathe(Actor actor) {
        if (reduced) {
            return;
        }
        actor.addAction(Actions.sequence(
            Actions.delay(0.6f), // after any entrance, which resets the transform flag at its end
            Actions.run(() -> {
                if (actor instanceof Group) {
                    ((Group) actor).setTransform(true);
                }
                actor.setOrigin(Align.center);
            }),
            Actions.forever(Actions.sequence(
                Actions.scaleTo(1.02f, 1.02f, 1.1f, Interpolation.sine),
                Actions.scaleTo(1f, 1f, 1.1f, Interpolation.sine)))));
    }

    /**
     * Counts a number label from {@code from} to {@code to} (up to 0.6 s), then pops
     * {@code popTarget} (the chip around it: a Label ignores its own scale).
     */
    public static void countTo(Label label, int from, int to, Actor popTarget) {
        if (reduced || from == to) {
            label.setText(String.valueOf(to));
            return;
        }
        float duration = Math.min(0.6f, 0.25f + Math.abs(to - from) * 0.004f);
        label.clearActions();
        label.addAction(Actions.sequence(
            CountUpAction.count(from, to, duration),
            Actions.run(() -> {
                if (popTarget != null) {
                    pop(popTarget);
                }
            })));
    }
}
