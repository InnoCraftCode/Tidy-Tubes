package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

/**
 * One ball on the board.
 *
 * <p>Every ball shares the single procedurally generated sphere texture and is tinted by the
 * batch, so the whole board is one draw call's worth of state changes and nothing is allocated
 * while rendering. The shading baked into that texture is what stops a flat tint from looking
 * like a coloured dot.</p>
 */
public final class BallActor extends Image {

    private final int colorIndex;

    public BallActor(Skin skin, int colorIndex, float size) {
        super(skin.getDrawable("ball"));
        this.colorIndex = colorIndex;
        setColor(Palette.ball(colorIndex));
        setSize(size, size);
        setOrigin(Align.center);
    }

    /** @return the model-level colour id this ball carries */
    public int colorIndex() {
        return colorIndex;
    }
}
