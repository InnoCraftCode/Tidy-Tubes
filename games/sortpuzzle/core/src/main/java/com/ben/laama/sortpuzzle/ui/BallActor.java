package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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
    private final BitmapFont labelFont;
    private final GlyphLayout labelLayout;
    private final String label;
    private final Color previousFontColor = new Color();

    public BallActor(Skin skin, int colorIndex, float size) {
        this(skin, colorIndex, size, false);
    }

    public BallActor(Skin skin, int colorIndex, float size, boolean showColorLabel) {
        super(skin.getDrawable("ball"));
        this.colorIndex = colorIndex;
        setColor(Palette.ball(colorIndex));
        setSize(size, size);
        setOrigin(Align.center);
        labelFont = showColorLabel ? skin.getFont("small") : null;
        label = showColorLabel ? String.valueOf(colorIndex + 1) : "";
        labelLayout = showColorLabel ? new GlyphLayout(labelFont, label) : null;
    }

    /** @return the model-level colour id this ball carries */
    public int colorIndex() {
        return colorIndex;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (labelFont == null) {
            return;
        }
        float x = getX() + (getWidth() - labelLayout.width) * 0.5f;
        float y = getY() + (getHeight() + labelLayout.height) * 0.5f;
        previousFontColor.set(labelFont.getColor());
        labelFont.setColor(0f, 0f, 0f, getColor().a * parentAlpha * 0.72f);
        labelFont.draw(batch, labelLayout, x + 1f, y - 1f);
        labelFont.setColor(1f, 1f, 1f, getColor().a * parentAlpha);
        labelFont.draw(batch, labelLayout, x, y);
        labelFont.setColor(previousFontColor);
    }
}
