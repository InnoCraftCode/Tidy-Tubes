package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

/**
 * Procedural texture factory — the game ships with no bitmap art, so every pixel the
 * renderer touches is generated here at start-up.
 *
 * <p>All shapes are produced white so they can be tinted per draw call; edges are
 * anti-aliased analytically from a signed distance field, which looks considerably better
 * than {@link Pixmap#fillCircle} and costs nothing at runtime because it happens once.</p>
 *
 * <p>Every returned {@link Texture} must be handed to a {@code Skin} (or otherwise disposed)
 * by the caller.</p>
 */
public final class Tex {

    private Tex() {
    }

    /** A signed distance: negative inside the shape, in pixels. */
    public interface Sdf {
        float at(float x, float y);
    }

    /** 1x1 opaque white pixel — the workhorse for tinted rectangles. */
    public static Texture white() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    /**
     * Rasterises any signed distance field into a white, anti-aliased texture.
     *
     * @param width  texture width in pixels
     * @param height texture height in pixels
     */
    public static Texture fromSdf(int width, int height, Sdf sdf) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float alpha = MathUtils.clamp(0.5f - sdf.at(x + 0.5f, y + 0.5f), 0f, 1f);
                pixmap.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, alpha));
            }
        }
        return upload(pixmap);
    }

    /**
     * Anti-aliased filled disc.
     *
     * @param diameter texture size in pixels
     */
    public static Texture circle(int diameter) {
        float radius = diameter * 0.5f;
        return fromSdf(diameter, diameter, (x, y) -> Sdfs.circle(x, y, radius, radius, radius));
    }

    /**
     * Anti-aliased disc with a soft vertical shading ramp, so a flat tint reads as a
     * three-dimensional ball without any lighting maths in the render loop.
     *
     * @param diameter  texture size in pixels
     * @param highlight strength of the top-left specular, 0..1
     */
    public static Texture shadedBall(int diameter, float highlight) {
        Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float radius = diameter * 0.5f;
        // Specular centre, up and to the left.
        float sx = radius * 0.64f;
        float sy = radius * 0.58f;
        float specularRadius = radius * 0.5f;

        for (int y = 0; y < diameter; y++) {
            for (int x = 0; x < diameter; x++) {
                float px = x + 0.5f;
                float py = y + 0.5f;
                float dx = px - radius;
                float dy = py - radius;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float alpha = MathUtils.clamp(radius - distance, 0f, 1f);
                if (alpha <= 0f) {
                    pixmap.drawPixel(x, y, 0);
                    continue;
                }
                // Base shading: brighter at the top, darker at the bottom rim.
                float vertical = 1f - (py / diameter);            // 1 at top, 0 at bottom
                float shade = 0.76f + 0.36f * vertical;
                // Darken the very rim to fake contact occlusion.
                float rim = MathUtils.clamp((radius - distance) / (radius * 0.24f), 0f, 1f);
                shade *= 0.66f + 0.34f * rim;
                // Specular blob.
                float sdx = px - sx;
                float sdy = py - sy;
                float specular = 1f - MathUtils.clamp(
                    (float) Math.sqrt(sdx * sdx + sdy * sdy) / specularRadius, 0f, 1f);
                float value = MathUtils.clamp(shade + highlight * specular * specular, 0f, 1f);
                pixmap.drawPixel(x, y, Color.rgba8888(value, value, value, alpha));
            }
        }
        return upload(pixmap);
    }

    /**
     * Anti-aliased rounded rectangle sized for use as a nine-patch: the returned texture is
     * {@code 2 * radius + 4} pixels square, leaving a 4px stretchable centre.
     */
    public static Texture roundRect(int radius) {
        int size = radius * 2 + 4;
        return roundRect(size, radius, 0f);
    }

    /**
     * Anti-aliased rounded rectangle outline sized for use as a nine-patch.
     *
     * @param thickness stroke width in pixels; {@code 0} produces a filled shape
     */
    public static Texture roundRectOutline(int radius, float thickness) {
        int size = radius * 2 + 4;
        return roundRect(size, radius, thickness);
    }

    private static Texture roundRect(int size, int radius, float thickness) {
        float half = size * 0.5f;
        return fromSdf(size, size, (x, y) -> {
            float d = Sdfs.roundBox(x, y, half, half, half, half, radius);
            return thickness <= 0f ? d : Math.abs(d + thickness * 0.5f) - thickness * 0.5f;
        });
    }

    /**
     * The glass tube: a U shape with a flat mouth. Produced white with a baked-in light ramp
     * (brighter along the left wall, a touch darker at the bottom) so a translucent tint reads
     * as glass rather than as a flat panel.
     *
     * @param width  tube width in pixels
     * @param height tube height in pixels
     */
    public static Texture tubeBody(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float radius = width * 0.5f;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float px = x + 0.5f;
                float py = y + 0.5f;
                float d = Sdfs.tube(px, py, width, height, radius);
                float alpha = MathUtils.clamp(0.5f - d, 0f, 1f);
                if (alpha <= 0f) {
                    pixmap.drawPixel(x, y, 0);
                    continue;
                }
                float u = px / width;                     // 0 left .. 1 right
                float v = py / height;                    // 0 top  .. 1 bottom (pixmap space)
                // Left-wall highlight, a faint right-wall reflection, a darker base.
                float light = 0.80f
                    + 0.45f * (float) Math.exp(-Math.pow((u - 0.18f) / 0.11f, 2))
                    + 0.16f * (float) Math.exp(-Math.pow((u - 0.84f) / 0.07f, 2))
                    - 0.18f * v * v;
                float value = MathUtils.clamp(light, 0f, 1f);
                pixmap.drawPixel(x, y, Color.rgba8888(value, value, value, alpha));
            }
        }
        return upload(pixmap);
    }

    /** Outline of {@link #tubeBody}, drawn as a thin stroke with a slightly heavier lip. */
    public static Texture tubeRim(int width, int height, float thickness) {
        float radius = width * 0.5f;
        return fromSdf(width, height, (x, y) -> {
            float d = Sdfs.tube(x, y, width, height, radius);
            float stroke = y < thickness * 2.2f ? thickness * 1.6f : thickness;
            return Math.abs(d + stroke * 0.5f) - stroke * 0.5f;
        });
    }

    /**
     * One-pixel-wide vertical gradient, row 0 being {@code top}. Drawn stretched across the
     * whole screen it costs a single quad per frame.
     */
    public static Texture verticalGradient(int height, Color top, Color bottom) {
        Pixmap pixmap = new Pixmap(1, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < height; y++) {
            float t = height == 1 ? 0f : y / (float) (height - 1);
            float r = MathUtils.lerp(top.r, bottom.r, t);
            float g = MathUtils.lerp(top.g, bottom.g, t);
            float b = MathUtils.lerp(top.b, bottom.b, t);
            float a = MathUtils.lerp(top.a, bottom.a, t);
            pixmap.drawPixel(0, y, Color.rgba8888(r, g, b, a));
        }
        return upload(pixmap);
    }

    /** Soft radial falloff, used for glows, shadows, the aurora washes and the win burst. */
    public static Texture radialGlow(int diameter, float falloff) {
        Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float radius = diameter * 0.5f;
        for (int y = 0; y < diameter; y++) {
            for (int x = 0; x < diameter; x++) {
                float dx = x + 0.5f - radius;
                float dy = y + 0.5f - radius;
                float t = MathUtils.clamp((float) Math.sqrt(dx * dx + dy * dy) / radius, 0f, 1f);
                float alpha = (float) Math.pow(1f - t, falloff);
                pixmap.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, alpha));
            }
        }
        return upload(pixmap);
    }

    private static Texture upload(Pixmap pixmap) {
        Texture texture = new Texture(pixmap, true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }
}
