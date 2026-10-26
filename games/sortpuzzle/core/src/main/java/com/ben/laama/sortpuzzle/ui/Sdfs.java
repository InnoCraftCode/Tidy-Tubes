package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.math.MathUtils;

/**
 * Signed-distance primitives (negative inside), in pixels, used by {@link Tex} and
 * {@link Icons} to rasterise shapes with analytic anti-aliasing.
 *
 * <p>Combine with {@link #union} / {@link #subtract}; turn a filled shape into a stroke with
 * {@link #stroke}. Nothing here allocates.</p>
 */
public final class Sdfs {

    private Sdfs() {
    }

    public static float circle(float x, float y, float cx, float cy, float r) {
        float dx = x - cx;
        float dy = y - cy;
        return (float) Math.sqrt(dx * dx + dy * dy) - r;
    }

    /** Axis-aligned box with rounded corners; {@code hw}/{@code hh} are half extents. */
    public static float roundBox(float x, float y, float cx, float cy, float hw, float hh,
                                 float round) {
        float qx = Math.abs(x - cx) - hw + round;
        float qy = Math.abs(y - cy) - hh + round;
        float outside = (float) Math.sqrt(Math.max(qx, 0f) * Math.max(qx, 0f)
            + Math.max(qy, 0f) * Math.max(qy, 0f));
        float inside = Math.min(Math.max(qx, qy), 0f);
        return outside + inside - round;
    }

    /** Line segment from (ax,ay) to (bx,by) with a round cap and the given half thickness. */
    public static float segment(float x, float y, float ax, float ay, float bx, float by,
                                float halfThickness) {
        float pax = x - ax;
        float pay = y - ay;
        float bax = bx - ax;
        float bay = by - ay;
        float h = MathUtils.clamp((pax * bax + pay * bay) / (bax * bax + bay * bay), 0f, 1f);
        float dx = pax - bax * h;
        float dy = pay - bay * h;
        return (float) Math.sqrt(dx * dx + dy * dy) - halfThickness;
    }

    /**
     * Circular arc of radius {@code r} centred on (cx,cy), from angle {@code from} sweeping
     * counter-clockwise by {@code sweep} radians (y axis pointing down, as in a Pixmap), with
     * round caps and the given half thickness. Angles are measured clockwise from +x because
     * the pixmap's y axis points down; callers only need consistency.
     */
    public static float arc(float x, float y, float cx, float cy, float r, float from,
                            float sweep, float halfThickness) {
        float dx = x - cx;
        float dy = y - cy;
        float angle = MathUtils.atan2(dy, dx);
        float rel = normalise(angle - from);
        if (rel >= 0f && rel <= sweep) {
            return Math.abs((float) Math.sqrt(dx * dx + dy * dy) - r) - halfThickness;
        }
        float ax = cx + MathUtils.cos(from) * r;
        float ay = cy + MathUtils.sin(from) * r;
        float bx = cx + MathUtils.cos(from + sweep) * r;
        float by = cy + MathUtils.sin(from + sweep) * r;
        return Math.min(circle(x, y, ax, ay, halfThickness), circle(x, y, bx, by, halfThickness));
    }

    private static float normalise(float angle) {
        while (angle < 0f) angle += MathUtils.PI2;
        while (angle >= MathUtils.PI2) angle -= MathUtils.PI2;
        return angle;
    }

    /**
     * Convex polygon given as a flat {@code x0,y0,x1,y1,...} list in clockwise order (in
     * pixmap space, y down). Exact inside; a close-enough approximation outside, which is all
     * anti-aliasing needs.
     */
    public static float convexPolygon(float x, float y, float... points) {
        float distance = -Float.MAX_VALUE;
        int n = points.length / 2;
        for (int i = 0; i < n; i++) {
            float ax = points[2 * i];
            float ay = points[2 * i + 1];
            float bx = points[2 * ((i + 1) % n)];
            float by = points[2 * ((i + 1) % n) + 1];
            float ex = bx - ax;
            float ey = by - ay;
            float length = (float) Math.sqrt(ex * ex + ey * ey);
            // Outward normal of a clockwise edge in y-down space.
            float nx = -ey / length;
            float ny = ex / length;
            float d = (x - ax) * nx + (y - ay) * ny;
            distance = Math.max(distance, d);
        }
        return distance;
    }

    /** Triangle pointing along +x with its base centred at (cx,cy): a plain arrowhead. */
    public static float arrowHead(float x, float y, float cx, float cy, float length,
                                  float halfBase, float angle) {
        float cos = MathUtils.cos(angle);
        float sin = MathUtils.sin(angle);
        // Rotate the sample point into the arrow's frame.
        float lx = (x - cx) * cos + (y - cy) * sin;
        float ly = -(x - cx) * sin + (y - cy) * cos;
        return convexPolygon(lx, ly, 0f, -halfBase, length, 0f, 0f, halfBase);
    }

    /** The glass tube outline: straight walls, a flat open top, and a semicircular bottom. */
    public static float tube(float x, float y, float width, float height, float bottomRadius) {
        float hw = width * 0.5f;
        float cy = height - bottomRadius;
        if (y <= cy) {
            // Walls: distance to the vertical strip, open at the top (a tiny 2px round only).
            float dx = Math.abs(x - hw) - hw;
            float dy = -y; // negative inside, 0 at the very top edge
            float outside = (float) Math.sqrt(Math.max(dx, 0f) * Math.max(dx, 0f)
                + Math.max(dy, 0f) * Math.max(dy, 0f));
            float inside = Math.min(Math.max(dx, dy), 0f);
            return outside + inside;
        }
        return circle(x, y, hw, cy, bottomRadius);
    }

    public static float union(float a, float b) {
        return Math.min(a, b);
    }

    public static float subtract(float a, float b) {
        return Math.max(a, -b);
    }

    /** Turns a filled distance into an outline of the given total thickness. */
    public static float stroke(float d, float thickness) {
        return Math.abs(d) - thickness * 0.5f;
    }
}
