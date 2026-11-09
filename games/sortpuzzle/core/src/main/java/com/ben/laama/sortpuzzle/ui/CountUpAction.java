package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

/**
 * Rolls a number {@link Label} from one value to another (pooled via {@link Actions#action}).
 * Only rewrites the text when the shown integer changes, so a long count does not rebuild the
 * glyph layout every frame.
 */
public final class CountUpAction extends TemporalAction {

    private int from;
    private int to;
    private int shown = Integer.MIN_VALUE;

    public static CountUpAction count(int from, int to, float duration) {
        CountUpAction action = Actions.action(CountUpAction.class);
        action.from = from;
        action.to = to;
        action.shown = Integer.MIN_VALUE;
        action.setDuration(duration);
        action.setInterpolation(Interpolation.pow2Out);
        return action;
    }

    @Override
    protected void update(float percent) {
        int value = Math.round(from + (to - from) * percent);
        if (value != shown && target instanceof Label) {
            shown = value;
            ((Label) target).setText(String.valueOf(value)); // only when the integer changes
        }
    }

    @Override
    public void reset() {
        super.reset();
        shown = Integer.MIN_VALUE;
    }
}
