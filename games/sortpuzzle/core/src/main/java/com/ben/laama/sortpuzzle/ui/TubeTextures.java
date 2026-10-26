package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Glass-tube drawables generated at the exact size a board needs.
 *
 * <p>A U-shaped tube cannot be a nine-patch — its bottom radius is half its width, which
 * changes from one level to the next — so each distinct size is rasterised once and cached.
 * A level uses one size, the home screen's brand mark another; the cache never grows past a
 * handful of entries and is disposed with the game.</p>
 */
public final class TubeTextures implements Disposable {

    private static final float RIM_THICKNESS = 3f;

    private final ObjectMap<String, Texture> textures = new ObjectMap<>();
    private final ObjectMap<String, Drawable> drawables = new ObjectMap<>();

    /** @return the shaded glass body, white, to be tinted */
    public Drawable body(int width, int height) {
        return get("body", width, height);
    }

    /** @return the rim stroke, white, to be tinted */
    public Drawable rim(int width, int height) {
        return get("rim", width, height);
    }

    private Drawable get(String kind, int width, int height) {
        String key = kind + ":" + width + "x" + height;
        Drawable drawable = drawables.get(key);
        if (drawable == null) {
            Texture texture = kind.equals("body")
                ? Tex.tubeBody(width, height)
                : Tex.tubeRim(width, height, RIM_THICKNESS);
            textures.put(key, texture);
            drawable = new TextureRegionDrawable(new TextureRegion(texture));
            drawables.put(key, drawable);
        }
        return drawable;
    }

    @Override
    public void dispose() {
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
        drawables.clear();
    }
}
