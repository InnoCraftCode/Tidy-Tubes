package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

/**
 * Moves an actor along an arc rather than a straight line.
 *
 * <p>Horizontal travel uses {@link Interpolation#swingOut} so the ball settles over its tube
 * with a little weight, while the vertical component adds a sine-shaped hop on top of the
 * straight interpolation. A plain {@code moveTo} makes balls slide through the glass; this
 * makes them look like they are being lifted out and dropped in.</p>
 *
 * <p>Obtained from the scene2d action pool, so a hundred moves cost nothing in garbage.</p>
 */
public final class ArcMoveAction extends TemporalAction {

    private float startX;
    private float startY;
    private float endX;
    private float endY;
    private float arcHeight;

    /** Required by the action pool. Use {@link #arcTo} instead. */
    public ArcMoveAction() {
    }

    /**
     * @param x         destination x, bottom-left corner
     * @param y         destination y, bottom-left corner
     * @param arcHeight how high above the straight line the peak of the hop sits
     * @param duration  seconds
     */
    public static ArcMoveAction arcTo(float x, float y, float arcHeight, float duration) {
        ArcMoveAction action = Actions.action(ArcMoveAction.class);
        action.endX = x;
        action.endY = y;
        action.arcHeight = arcHeight;
        action.setDuration(duration);
        action.setInterpolation(null);
        return action;
    }

    @Override
    protected void begin() {
        startX = target.getX();
        startY = target.getY();
    }

    @Override
    protected void update(float percent) {
        float x = Interpolation.swingOut.apply(startX, endX, percent);
        float y = Interpolation.linear.apply(startY, endY, percent)
            + arcHeight * MathUtils.sin(MathUtils.PI * percent);
        target.setPosition(x, y);
    }

    @Override
    public void reset() {
        super.reset();
        startX = 0f;
        startY = 0f;
        endX = 0f;
        endY = 0f;
        arcHeight = 0f;
    }
}
