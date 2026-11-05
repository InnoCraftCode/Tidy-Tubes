package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

import com.ben.laama.sortpuzzle.logic.Cosmetic;

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
    private static Pixmap shadedBallPixmap(int diameter, float highlight) {
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
        return pixmap;
    }

    /**
     * The ball sprite for a shop finish (see {@link com.ben.laama.sortpuzzle.logic.Cosmetic}):
     * {@code finish.glossy} is {@link #shadedBall} as shipped; the others reshape the baked
     * lighting only, so a flat tint still does all the colouring at draw time.
     *
     * @param diameter texture size in pixels
     * @param finishId a {@code finish.*} catalogue id; unknown ids fall back to glossy
     */
    public static Texture ball(int diameter, String finishId) {
        Cosmetic finish = Cosmetic.byId(finishId);
        String base = finish != null && finish.finishBase() != null ? finish.finishBase() : finishId;
        Pixmap pixmap = basePixmap(diameter, base);
        if (finish != null && finish.pattern() != Cosmetic.Pattern.NONE) {
            applyPattern(pixmap, finish.pattern(), finish.patternCount(), finish.patternStrength());
        }
        return upload(pixmap);
    }

    /** The plain glossy ball, the sprite every profile starts with. */
    public static Texture shadedBall(int diameter, float highlight) {
        return upload(shadedBallPixmap(diameter, highlight));
    }

    /** One of the six hand-made lighting models; unknown ids fall back to glossy. */
    private static Pixmap basePixmap(int diameter, String baseId) {
        if ("finish.matte".equals(baseId)) return matteBall(diameter);
        if ("finish.glass".equals(baseId)) return glassBall(diameter);
        if ("finish.gem".equals(baseId)) return gemBall(diameter);
        if ("finish.jelly".equals(baseId)) return jellyBall(diameter);
        if ("finish.chrome".equals(baseId)) return chromeBall(diameter);
        return shadedBallPixmap(diameter, 0.6f);
    }

    /**
     * Lays a pattern over a baked grey ball. Coordinates are mapped onto the sphere (latitude /
     * longitude), so bands and dots curve with the surface. Patterns darken by at most ~40 %
     * (sparkles lighten), so the tint, i.e. the ball's colour, stays the dominant cue.
     */
    private static void applyPattern(Pixmap pixmap, Cosmetic.Pattern pattern, int count, float strength) {
        int diameter = pixmap.getWidth();
        float radius = diameter * 0.5f;
        float darken = Math.min(0.45f, strength * 1.5f);
        for (int y = 0; y < diameter; y++) {
            for (int x = 0; x < diameter; x++) {
                int pixel = pixmap.getPixel(x, y);
                float alpha = (pixel & 0xFF) / 255f;
                if (alpha <= 0f) continue;
                float nx = (x + 0.5f - radius) / radius;
                float ny = (radius - y - 0.5f) / radius;          // up is positive
                float rr = nx * nx + ny * ny;
                if (rr >= 1f) continue;
                float nz = (float) Math.sqrt(1f - rr);
                float lat = (float) Math.asin(MathUtils.clamp(ny, -1f, 1f));
                float lon = MathUtils.atan2(nx, nz);
                float mask;
                boolean lighten = false;
                switch (pattern) {
                    case BANDS:
                        mask = soft((float) Math.sin(lat * count * 2f));
                        break;
                    case STRIPES:
                        mask = soft((float) Math.sin((lon * 0.8f + lat) * count * 1.6f));
                        break;
                    case RING:
                        mask = soft((0.2f * count - Math.abs(lat)) * 5f);
                        break;
                    case DOTS: {
                        float cell = MathUtils.PI / (count + 1f);
                        float u = lon / cell + 100f;
                        float v = lat / cell + 100f;
                        float du = u - MathUtils.floor(u) - 0.5f;
                        float dv = v - MathUtils.floor(v) - 0.5f;
                        mask = soft((0.3f - (float) Math.sqrt(du * du + dv * dv)) * 8f);
                        break;
                    }
                    case SWIRL:
                        mask = soft((float) Math.sin(MathUtils.atan2(ny, nx) * count
                            + (float) Math.sqrt(rr) * 7f));
                        break;
                    case DUO:
                        mask = count <= 1 ? soft(-ny * 6f) : soft(-(nx + ny) * 4f);
                        break;
                    case CHECKER:
                        mask = soft((float) (Math.sin(lon * count * 2f) * Math.sin(lat * count * 2f)) * 2f);
                        break;
                    case SPARKLE: {
                        float cells = (float) Math.sqrt(count) * 2f;
                        float u = (nx + 1f) * cells;
                        float v = (ny + 1f) * cells;
                        int cu = MathUtils.floor(u);
                        int cv = MathUtils.floor(v);
                        float hash = hash(cu, cv);
                        if (hash < 0.45f) {
                            mask = 0f;
                        } else {
                            float ox = 0.25f + 0.5f * hash(cv, cu);
                            float oy = 0.25f + 0.5f * hash(cu + 7, cv + 3);
                            float du = u - cu - ox;
                            float dv = v - cv - oy;
                            mask = soft((0.12f - (float) Math.sqrt(du * du + dv * dv)) * 20f);
                        }
                        lighten = true;
                        break;
                    }
                    default:
                        mask = 0f;
                        break;
                }
                if (mask <= 0f) continue;
                float value = ((pixel >>> 24) & 0xFF) / 255f;
                value = lighten ? Math.min(1f, value + 0.55f * mask)
                    : value * (1f - darken * mask);
                pixmap.drawPixel(x, y, Color.rgba8888(value, value, value, alpha));
            }
        }
    }

    /** Signed field to a soft 0..1 mask: a few pixels of anti-aliasing at any texture size. */
    private static float soft(float signed) {
        return MathUtils.clamp(0.5f + signed * 3f, 0f, 1f);
    }

    /** Deterministic 0..1 hash of a cell, so sparkles land in the same places every time. */
    private static float hash(int a, int b) {
        double h = Math.sin(a * 12.9898 + b * 78.233) * 43758.5453;
        return (float) (h - Math.floor(h));
    }

    /**
     * Jelly: a bright, almost flat core that darkens towards a thick soft edge (light scattering
     * inside), with a wide, low highlight sitting high and a faint bottom bounce.
     */
    private static Pixmap jellyBall(int diameter) {
        Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float radius = diameter * 0.5f;
        float sx = radius * 0.78f;
        float sy = radius * 0.5f;
        float specularRadius = radius * 0.62f;
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
                float t = distance / radius;
                float vertical = 1f - (py / diameter);
                float core = 1f - t * t;                            // bright middle
                float shade = 0.62f + 0.3f * core + 0.08f * vertical;
                float edge = MathUtils.clamp((t - 0.7f) / 0.3f, 0f, 1f);
                shade *= 1f - 0.3f * edge * edge;                    // soft dark rim
                float bounce = MathUtils.clamp((0.25f - vertical) / 0.25f, 0f, 1f) * edge;
                float sdx = px - sx;
                float sdy = py - sy;
                float specular = 1f - MathUtils.clamp(
                    (float) Math.sqrt(sdx * sdx + sdy * sdy) / specularRadius, 0f, 1f);
                float value = MathUtils.clamp(shade + 0.28f * specular * specular + 0.12f * bounce, 0f, 1f);
                pixmap.drawPixel(x, y, Color.rgba8888(value, value, value, alpha));
            }
        }
        return pixmap;
    }

    /**
     * Chrome: a mirrored "sky over ground" reflection - a bright upper half, a sharp horizon
     * just below the middle, a darker lower half with a thin bright line near the base - plus
     * a hard, small specular. Reads as polished metal under any tint.
     */
    private static Pixmap chromeBall(int diameter) {
        Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float radius = diameter * 0.5f;
        float sx = radius * 0.66f;
        float sy = radius * 0.5f;
        float specularRadius = radius * 0.26f;
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
                // Sphere-mapped vertical coordinate: the reflection curves with the surface.
                float ny = MathUtils.clamp(-dy / radius, -1f, 1f);          // 1 top .. -1 bottom
                float horizon = 0.08f;
                float shade;
                if (ny > horizon) {
                    float t = (ny - horizon) / (1f - horizon);               // 0 at horizon .. 1 top
                    shade = 0.62f + 0.36f * t;                               // sky: bright, brighter up
                } else {
                    float t = (horizon - ny) / (1f + horizon);               // 0 at horizon .. 1 bottom
                    shade = 0.28f + 0.2f * t;                                // ground: dark
                    float line = 1f - MathUtils.clamp(Math.abs(t - 0.78f) / 0.07f, 0f, 1f);
                    shade += 0.42f * line;                                   // bright base line
                }
                float band = 1f - MathUtils.clamp(Math.abs(ny - horizon) / 0.04f, 0f, 1f);
                shade = MathUtils.lerp(shade, 0.96f, band * 0.9f);           // the horizon itself
                float contact = MathUtils.clamp((radius - distance) / (radius * 0.07f), 0f, 1f);
                shade *= 0.5f + 0.5f * contact;
                float sdx = px - sx;
                float sdy = py - sy;
                float specular = 1f - MathUtils.clamp(
                    (float) Math.sqrt(sdx * sdx + sdy * sdy) / specularRadius, 0f, 1f);
                float value = MathUtils.clamp(shade + 0.9f * specular * specular * specular, 0f, 1f);
                pixmap.drawPixel(x, y, Color.rgba8888(value, value, value, alpha));
            }
        }
        return pixmap;
    }

    /** Flat, soft shading: no specular, a gentle top-to-bottom ramp, a faint contact rim. */
    private static Pixmap matteBall(int diameter) {
        Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float radius = diameter * 0.5f;
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
                float vertical = 1f - (py / diameter);
                float shade = 0.86f + 0.18f * vertical;
                float rim = MathUtils.clamp((radius - distance) / (radius * 0.3f), 0f, 1f);
                shade *= 0.82f + 0.18f * rim;
                float value = MathUtils.clamp(shade, 0f, 1f);
                pixmap.drawPixel(x, y, Color.rgba8888(value, value, value, alpha));
            }
        }
        return pixmap;
    }

    /**
     * Glass: a hard, small specular up-left, a dark core, a bright fresnel band along the
     * lower edge and a second faint reflection down-right.
     */
    private static Pixmap glassBall(int diameter) {
        Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float radius = diameter * 0.5f;
        float sx = radius * 0.62f;
        float sy = radius * 0.56f;
        float specularRadius = radius * 0.34f;
        float rx = radius * 1.36f;
        float ry = radius * 1.42f;
        float reflectRadius = radius * 0.3f;
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
                float t = distance / radius;                       // 0 centre .. 1 edge
                float vertical = 1f - (py / diameter);
                float shade = 0.58f + 0.26f * vertical;
                // Dark core, bright rim: the look of light bending through a sphere.
                float fresnel = MathUtils.clamp((t - 0.62f) / 0.3f, 0f, 1f);
                shade += 0.34f * fresnel * fresnel * (0.4f + 0.6f * (1f - vertical));
                float contact = MathUtils.clamp((radius - distance) / (radius * 0.08f), 0f, 1f);
                shade *= 0.55f + 0.45f * contact;
                float sdx = px - sx;
                float sdy = py - sy;
                float specular = 1f - MathUtils.clamp(
                    (float) Math.sqrt(sdx * sdx + sdy * sdy) / specularRadius, 0f, 1f);
                float rdx = px - rx;
                float rdy = py - ry;
                float reflect = 1f - MathUtils.clamp(
                    (float) Math.sqrt(rdx * rdx + rdy * rdy) / reflectRadius, 0f, 1f);
                float value = MathUtils.clamp(
                    shade + 1.1f * specular * specular * specular + 0.3f * reflect * reflect, 0f, 1f);
                pixmap.drawPixel(x, y, Color.rgba8888(value, value, value, alpha));
            }
        }
        return pixmap;
    }

    /**
     * Gem: a flat bright "table" in the middle and eight crisp facets around it, each lit a
     * little differently, plus the glossy specular so it still reads as the same ball.
     */
    private static Pixmap gemBall(int diameter) {
        Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float radius = diameter * 0.5f;
        float table = radius * 0.46f;
        float sx = radius * 0.7f;
        float sy = radius * 0.62f;
        float specularRadius = radius * 0.4f;
        // Facet brightness around the ring: lit from the top-left, darkest bottom-right.
        float[] facet = {1.00f, 0.90f, 0.72f, 0.60f, 0.52f, 0.62f, 0.78f, 0.94f};
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
                float shade;
                if (distance < table) {
                    shade = 0.96f;
                } else {
                    // Angle measured from the top-left light, clockwise in pixmap space.
                    float angle = MathUtils.atan2(dy, dx) + MathUtils.PI * 0.75f;
                    int sector = Math.floorMod((int) Math.floor(angle / (MathUtils.PI2 / 8f)), 8);
                    shade = facet[sector];
                    // A thin bright line where facet meets table, a darker one at the girdle.
                    float edge = (distance - table) / (radius - table);
                    if (edge < 0.08f) shade = Math.min(1f, shade + 0.16f);
                    float contact = MathUtils.clamp((radius - distance) / (radius * 0.1f), 0f, 1f);
                    shade *= 0.6f + 0.4f * contact;
                }
                float sdx = px - sx;
                float sdy = py - sy;
                float specular = 1f - MathUtils.clamp(
                    (float) Math.sqrt(sdx * sdx + sdy * sdy) / specularRadius, 0f, 1f);
                float value = MathUtils.clamp(shade + 0.35f * specular * specular, 0f, 1f);
                pixmap.drawPixel(x, y, Color.rgba8888(value, value, value, alpha));
            }
        }
        return pixmap;
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
