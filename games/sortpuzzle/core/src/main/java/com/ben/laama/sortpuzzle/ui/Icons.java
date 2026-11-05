package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

/**
 * The icon set, rasterised once from signed distance fields at {@link #SIZE} pixels and drawn
 * scaled down. Everything is white and tinted at draw time, strokes are {@link #STROKE} px, and
 * every glyph fits the central 72x72 of the 96x96 canvas so they all sit on the same optical
 * weight. Names are what the {@code Skin} registers them under.
 */
public final class Icons {

    public static final int SIZE = 96;
    private static final float C = SIZE * 0.5f;
    private static final float STROKE = 8f;
    private static final float HALF = STROKE * 0.5f;

    public static final String[] NAMES = {
        "icon-back", "icon-close", "icon-undo", "icon-restart", "icon-hint", "icon-tube",
        "icon-settings", "icon-home", "icon-sound", "icon-sound-off", "icon-music", "icon-music-off",
        "icon-check", "icon-lock", "icon-coin", "icon-play", "icon-grid", "icon-info",
        "icon-next", "icon-replay", "icon-help", "icon-trash", "icon-star", "icon-shop", "icon-goal",
        "icon-share"
    };

    private Icons() {
    }

    /** @throws IllegalArgumentException for a name not in {@link #NAMES} */
    public static Texture render(String name) {
        switch (name) {
            case "icon-back":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.segment(x, y, 60, 22, 34, C, HALF),
                    Sdfs.segment(x, y, 34, C, 60, 74, HALF)));
            case "icon-next":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.segment(x, y, 38, 22, 64, C, HALF),
                    Sdfs.segment(x, y, 64, C, 38, 74, HALF)));
            case "icon-close":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.segment(x, y, 28, 28, 68, 68, HALF),
                    Sdfs.segment(x, y, 68, 28, 28, 68, HALF)));
            case "icon-undo":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> curvedArrow(x, y, false));
            case "icon-restart":
            case "icon-replay":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> curvedArrow(x, y, true));
            case "icon-hint":
                return Tex.fromSdf(SIZE, SIZE, Icons::bulb);
            case "icon-star":
                return Tex.fromSdf(SIZE, SIZE, Icons::star);
            case "icon-tube":
                return Tex.fromSdf(SIZE, SIZE, Icons::tubePlus);
            case "icon-settings":
                return Tex.fromSdf(SIZE, SIZE, Icons::gear);
            case "icon-home":
                return Tex.fromSdf(SIZE, SIZE, Icons::house);
            case "icon-sound":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(speaker(x, y),
                    Sdfs.union(
                        Sdfs.arc(x, y, 46, C, 16, -0.75f, 1.5f, HALF * 0.8f),
                        Sdfs.arc(x, y, 46, C, 30, -0.75f, 1.5f, HALF * 0.8f))));
            case "icon-sound-off":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(speaker(x, y),
                    Sdfs.union(
                        Sdfs.segment(x, y, 58, 36, 80, 60, HALF * 0.8f),
                        Sdfs.segment(x, y, 80, 36, 58, 60, HALF * 0.8f))));
            case "icon-music":
                return Tex.fromSdf(SIZE, SIZE, Icons::musicNote);
            case "icon-music-off":
                // The note with a diagonal slash; a gap is cut around the slash so it reads
                // as "crossed out" at 34 px instead of merging into the stems.
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.subtract(musicNote(x, y), Sdfs.segment(x, y, 18, 18, 80, 80, HALF * 2.2f)),
                    Sdfs.segment(x, y, 18, 18, 80, 80, HALF * 0.8f)));
            case "icon-check":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.segment(x, y, 24, 50, 42, 68, HALF),
                    Sdfs.segment(x, y, 42, 68, 74, 30, HALF)));
            case "icon-lock":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.roundBox(x, y, C, 60, 20, 15, 6),
                    Sdfs.arc(x, y, C, 42, 13, MathUtils.PI, MathUtils.PI, HALF * 0.75f)));
            case "icon-coin":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.stroke(Sdfs.circle(x, y, C, C, 30), STROKE),
                    Sdfs.roundBox(x, y, C, C, 7, 14, 3)));
            case "icon-play":
                return Tex.fromSdf(SIZE, SIZE, (x, y) ->
                    Sdfs.convexPolygon(x, y, 34, 22, 72, C, 34, 74) - 2f);
            case "icon-grid":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.union(Sdfs.roundBox(x, y, 34, 34, 13, 13, 5),
                        Sdfs.roundBox(x, y, 62, 34, 13, 13, 5)),
                    Sdfs.union(Sdfs.roundBox(x, y, 34, 62, 13, 13, 5),
                        Sdfs.roundBox(x, y, 62, 62, 13, 13, 5))));
            case "icon-info":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.stroke(Sdfs.circle(x, y, C, C, 30), STROKE * 0.8f),
                    Sdfs.union(Sdfs.circle(x, y, C, 34, 4.5f),
                        Sdfs.roundBox(x, y, C, 55, 3.5f, 12, 3))));
            case "icon-help":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.stroke(Sdfs.circle(x, y, C, C, 30), STROKE * 0.8f),
                    Sdfs.union(Sdfs.circle(x, y, C, 64, 4.5f),
                        Sdfs.union(Sdfs.arc(x, y, C, 40, 10, -MathUtils.PI, MathUtils.PI * 1.5f, HALF * 0.7f),
                            Sdfs.segment(x, y, C, 50, C, 54, HALF * 0.7f)))));
            case "icon-trash":
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.union(Sdfs.stroke(Sdfs.roundBox(x, y, C, 58, 18, 20, 5), STROKE * 0.8f),
                        Sdfs.segment(x, y, 24, 32, 72, 32, HALF * 0.8f)),
                    Sdfs.union(Sdfs.segment(x, y, 40, 22, 56, 22, HALF * 0.8f),
                        Sdfs.union(Sdfs.segment(x, y, 42, 46, 42, 68, HALF * 0.55f),
                            Sdfs.segment(x, y, 54, 46, 54, 68, HALF * 0.55f)))));
            case "icon-shop":
                // A shopping bag: rounded body, two handles, a short fold line below the mouth.
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.union(Sdfs.stroke(Sdfs.roundBox(x, y, C, 56, 22, 20, 6), STROKE * 0.8f),
                        Sdfs.arc(x, y, C, 38, 11, MathUtils.PI, MathUtils.PI, HALF * 0.75f)),
                    Sdfs.segment(x, y, 30, 46, 66, 46, HALF * 0.5f)));
            case "icon-share":
                // An open tray with an arrow rising out of it.
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.union(Sdfs.segment(x, y, C, 24, C, 58, HALF * 0.8f),
                        Sdfs.union(Sdfs.segment(x, y, 36, 38, C, 24, HALF * 0.8f),
                            Sdfs.segment(x, y, C, 24, 60, 38, HALF * 0.8f))),
                    Sdfs.union(Sdfs.segment(x, y, 28, 50, 28, 74, HALF * 0.8f),
                        Sdfs.union(Sdfs.segment(x, y, 28, 74, 68, 74, HALF * 0.8f),
                            Sdfs.segment(x, y, 68, 50, 68, 74, HALF * 0.8f)))));
            case "icon-goal":
                // A target: outer ring, inner ring, centre dot.
                return Tex.fromSdf(SIZE, SIZE, (x, y) -> Sdfs.union(
                    Sdfs.union(Sdfs.stroke(Sdfs.circle(x, y, C, C, 30), STROKE * 0.8f),
                        Sdfs.stroke(Sdfs.circle(x, y, C, C, 17), STROKE * 0.7f)),
                    Sdfs.circle(x, y, C, C, 5f)));
            default:
                throw new IllegalArgumentException("Unknown icon " + name);
        }
    }

    // ------------------------------------------------------------- shapes

    /** A three-quarter arc with an arrowhead; mirrored for restart. */
    private static float curvedArrow(float x, float y, boolean mirrored) {
        float sx = mirrored ? SIZE - x : x;
        float r = 24f;
        float from = -0.35f * MathUtils.PI;        // start upper-right
        float sweep = 1.45f * MathUtils.PI;         // sweep round the bottom to the left
        float arc = Sdfs.arc(sx, y, C, C + 2, r, from, sweep, HALF);
        float endAngle = from;                      // arrowhead at the start, pointing along -t
        float hx = C + MathUtils.cos(endAngle) * r;
        float hy = C + 2 + MathUtils.sin(endAngle) * r;
        float tangent = endAngle - MathUtils.HALF_PI; // pointing "backwards" along the sweep
        float head = Sdfs.arrowHead(sx, y, hx, hy, 20f, 13f, tangent);
        return Sdfs.union(arc, head);
    }

    private static float bulb(float x, float y) {
        float globe = Sdfs.stroke(Sdfs.circle(x, y, C, 40, 22), STROKE * 0.85f);
        float neck = Sdfs.roundBox(x, y, C, 66, 11, 4, 3);
        float base = Sdfs.roundBox(x, y, C, 76, 8, 3, 2.5f);
        float ray = Sdfs.union(
            Sdfs.segment(x, y, 14, 40, 6, 40, HALF * 0.6f),
            Sdfs.union(Sdfs.segment(x, y, 82, 40, 90, 40, HALF * 0.6f),
                Sdfs.union(Sdfs.segment(x, y, 24, 18, 18, 12, HALF * 0.6f),
                    Sdfs.segment(x, y, 72, 18, 78, 12, HALF * 0.6f))));
        return Sdfs.union(Sdfs.union(globe, neck), Sdfs.union(base, ray));
    }

    private static float tubePlus(float x, float y) {
        float body = Sdfs.stroke(Sdfs.tube(x - 20, y - 22, 28, 56, 14), STROKE * 0.8f);
        // Clip the stroke at the open top so the mouth stays open.
        float clip = y < 27f ? 10f : body;
        float plus = Sdfs.union(
            Sdfs.segment(x, y, 64, 20, 64, 44, HALF * 0.8f),
            Sdfs.segment(x, y, 52, 32, 76, 32, HALF * 0.8f));
        return Sdfs.union(clip, plus);
    }

    private static float gear(float x, float y) {
        float ring = Sdfs.stroke(Sdfs.circle(x, y, C, C, 20), STROKE * 1.4f);
        float teeth = Float.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            float a = i * MathUtils.PI2 / 8f;
            float cos = MathUtils.cos(a);
            float sin = MathUtils.sin(a);
            float lx = (x - C) * cos + (y - C) * sin;
            float ly = -(x - C) * sin + (y - C) * cos;
            teeth = Math.min(teeth, Sdfs.roundBox(lx, ly, 28, 0, 7, 6, 2.5f));
        }
        return Sdfs.union(ring, teeth);
    }

    /**
     * Filled five-point star, tip up. A star is not convex, so it is the union of five convex
     * kites (centre, inner point, tip, inner point). The existing glyphs only ever stroke
     * {@link Sdfs#convexPolygon}, which hides its sign; for a filled shape the points must run
     * centre, right inner, tip, left inner (counter-clockwise on screen) to come out negative inside.
     */
    private static float star(float x, float y) {
        float outer = 36f;
        float inner = 15f;
        float d = Float.MAX_VALUE;
        for (int k = 0; k < 5; k++) {
            float tip = -MathUtils.HALF_PI + k * MathUtils.PI2 / 5f;   // y grows downwards
            float left = tip - MathUtils.PI / 5f;
            float right = tip + MathUtils.PI / 5f;
            float kite = Sdfs.convexPolygon(x, y,
                C, C,
                C + MathUtils.cos(right) * inner, C + MathUtils.sin(right) * inner,
                C + MathUtils.cos(tip) * outer, C + MathUtils.sin(tip) * outer,
                C + MathUtils.cos(left) * inner, C + MathUtils.sin(left) * inner);
            d = Sdfs.union(d, kite);
        }
        return d - 1.5f; // slightly rounded corners
    }

    private static float house(float x, float y) {
        float roof = Sdfs.convexPolygon(x, y, C, 18, 82, 50, 14, 50);
        float roofStroke = Sdfs.stroke(roof, STROKE * 0.8f);
        float walls = Sdfs.stroke(Sdfs.roundBox(x, y, C, 62, 24, 18, 3), STROKE * 0.8f);
        float wallsOpenTop = y < 48f ? 10f : walls;
        float door = Sdfs.roundBox(x, y, C, 70, 6, 10, 2);
        return Sdfs.union(Sdfs.union(roofStroke, wallsOpenTop), door);
    }

    /** Two beamed eighth notes. */
    private static float musicNote(float x, float y) {
        return Sdfs.union(
            Sdfs.union(Sdfs.circle(x, y, 36, 66, 11), Sdfs.circle(x, y, 64, 60, 11)),
            Sdfs.union(
                Sdfs.segment(x, y, 45, 66, 45, 26, HALF * 0.8f),
                Sdfs.union(Sdfs.segment(x, y, 73, 60, 73, 20, HALF * 0.8f),
                    Sdfs.segment(x, y, 45, 26, 73, 20, HALF * 0.9f))));
    }

    private static float speaker(float x, float y) {
        float box = Sdfs.roundBox(x, y, 26, C, 8, 11, 2);
        float cone = Sdfs.convexPolygon(x, y, 30, 37, 50, 22, 50, 74, 30, 59);
        return Sdfs.union(box, cone);
    }
}
