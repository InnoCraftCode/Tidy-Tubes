package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * The country flags of the language picker, painted at start-up like every other shape in the
 * game (no image files, no emoji: a {@code BitmapFont} cannot draw regional-indicator pairs).
 * Simplified but recognisable at 40-60 units: 3:2, rounded corners, a faint edge so the white
 * parts of a flag do not dissolve into a light card.
 */
public final class Flags {

    /** Flag codes painted here; skin drawables are named {@code flag-<code>}. */
    public static final String[] CODES = {"GB", "FR", "SA", "ES", "BR", "DE", "ID", "TR"};

    private static final int WIDTH = 120;
    private static final int HEIGHT = 80;
    private static final int SAMPLES = 3;

    private Flags() {
    }

    private interface Paint {
        /** @return ARGB colour at (x, y) in flag-height units: x in [0, 1.5), y in [0, 1) from the top */
        int at(float x, float y);
    }

    public static Texture render(String code) {
        Paint paint = paint(code);
        Pixmap pixmap = new Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float radius = 9f;
        for (int py = 0; py < HEIGHT; py++) {
            for (int px = 0; px < WIDTH; px++) {
                float r = 0f, g = 0f, b = 0f;
                for (int sy = 0; sy < SAMPLES; sy++) {
                    for (int sx = 0; sx < SAMPLES; sx++) {
                        float fx = px + (sx + 0.5f) / SAMPLES;
                        float fy = py + (sy + 0.5f) / SAMPLES;
                        int c = paint.at(fx / HEIGHT, fy / HEIGHT);
                        r += ((c >> 16) & 0xFF);
                        g += ((c >> 8) & 0xFF);
                        b += (c & 0xFF);
                    }
                }
                int n = SAMPLES * SAMPLES;
                float d = Sdfs.roundBox(px + 0.5f, py + 0.5f, WIDTH * 0.5f, HEIGHT * 0.5f,
                    WIDTH * 0.5f, HEIGHT * 0.5f, radius);
                float alpha = Math.max(0f, Math.min(1f, 0.5f - d));
                // faint dark edge inside the outline
                float edge = Math.max(0f, Math.min(1f, 1.5f - Math.abs(d + 1f))) * 0.35f;
                float k = 1f - edge;
                pixmap.drawPixel(px, py, Color.rgba8888(
                    r / n / 255f * k, g / n / 255f * k, b / n / 255f * k, alpha));
            }
        }
        Texture texture = new Texture(pixmap, true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private static Paint paint(String code) {
        switch (code) {
            case "GB": return Flags::unitedKingdom;
            case "FR": return (x, y) -> x < 0.5f ? 0x0055A4 : x < 1f ? 0xFFFFFF : 0xEF4135;
            case "SA": return Flags::saudiArabia;
            case "ES": return (x, y) -> y < 0.25f || y >= 0.75f ? 0xAA151B : 0xF1BF00;
            case "BR": return Flags::brazil;
            case "DE": return (x, y) -> y < 1f / 3f ? 0x000000 : y < 2f / 3f ? 0xDD0000 : 0xFFCE00;
            case "ID": return (x, y) -> y < 0.5f ? 0xCE1126 : 0xFFFFFF;
            case "TR": return Flags::turkey;
            default: return (x, y) -> 0x888888;
        }
    }

    private static int unitedKingdom(float x, float y) {
        float cx = 0.75f;
        float cy = 0.5f;
        if (Math.abs(y - cy) < 0.1f || Math.abs(x - cx) < 0.1f) return 0xC8102E;
        if (Math.abs(y - cy) < 1f / 6f || Math.abs(x - cx) < 1f / 6f) return 0xFFFFFF;
        float norm = (float) Math.sqrt(1.5f * 1.5f + 1f);
        float d1 = Math.abs(y * 1.5f - x) / norm;
        float d2 = Math.abs((1f - y) * 1.5f - x) / norm;
        float d = Math.min(d1, d2);
        if (d < 0.035f) return 0xC8102E;
        if (d < 0.1f) return 0xFFFFFF;
        return 0x012169;
    }

    private static int saudiArabia(float x, float y) {
        int green = 0x006C35;
        int white = 0xFFFFFF;
        // the shahada, suggested by a band of calligraphic strokes
        if (y > 0.26f && y < 0.46f && x > 0.3f && x < 1.2f) {
            float wave = (float) Math.sin(x * 38f) * 0.05f + (float) Math.sin(x * 11f) * 0.03f;
            if (Math.abs(y - 0.40f - wave * 0.4f) < 0.022f) return white;          // baseline stroke
            if ((int) (x * 26f) % 3 == 0 && Math.abs(((x * 26f) % 1f) - 0.5f) < 0.22f
                && y > 0.28f && y < 0.41f) return white;                          // risers
        }
        // the sword: blade, guard and hilt
        if (y > 0.62f && y < 0.665f && x > 0.33f && x < 1.08f) return white;
        if (x > 1.0f && x < 1.035f && y > 0.57f && y < 0.715f) return white;
        if (y > 0.6f && y < 0.685f && x > 1.035f && x < 1.17f) return white;
        return green;
    }

    private static int brazil(float x, float y) {
        float cx = 0.75f;
        float cy = 0.5f;
        float dx = x - cx;
        float dy = y - cy;
        float circle = (float) Math.sqrt(dx * dx + dy * dy);
        if (circle < 0.25f) {
            // the white motto band: an arc across the globe
            float ax = x - 0.62f;
            float ay = y - 1.05f;
            float arc = (float) Math.sqrt(ax * ax + ay * ay);
            return Math.abs(arc - 0.6f) < 0.03f ? 0xFFFFFF : 0x002776;
        }
        if (Math.abs(dx) / 0.64f + Math.abs(dy) / 0.4f <= 1f) return 0xFFDF00;
        return 0x009C3B;
    }

    private static int turkey(float x, float y) {
        int red = 0xE30A17;
        float ox = x - 0.5f;
        float oy = y - 0.5f;
        float ix = x - 0.5625f;
        boolean outer = ox * ox + oy * oy < 0.25f * 0.25f;
        boolean inner = ix * ix + oy * oy < 0.2f * 0.2f;
        if (outer && !inner) return 0xFFFFFF;
        if (inStar(x, y, 0.79f, 0.5f, 0.125f)) return 0xFFFFFF;
        return red;
    }

    /** Five-pointed star, one tip pointing left (towards the crescent's opening). */
    private static boolean inStar(float x, float y, float cx, float cy, float outer) {
        float inner = outer * 0.382f;
        float[] vx = new float[10];
        float[] vy = new float[10];
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI + i * Math.PI / 5.0;
            float r = (i % 2 == 0) ? outer : inner;
            vx[i] = cx + (float) Math.cos(angle) * r;
            vy[i] = cy + (float) Math.sin(angle) * r;
        }
        boolean inside = false;
        for (int i = 0, j = 9; i < 10; j = i++) {
            if ((vy[i] > y) != (vy[j] > y)
                && x < (vx[j] - vx[i]) * (y - vy[i]) / (vy[j] - vy[i]) + vx[i]) {
                inside = !inside;
            }
        }
        return inside;
    }
}
