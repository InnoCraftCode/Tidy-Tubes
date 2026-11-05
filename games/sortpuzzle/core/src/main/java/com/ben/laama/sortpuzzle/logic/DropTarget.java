package com.ben.laama.sortpuzzle.logic;

/**
 * Decides which tube a dragged run of balls was dropped on.
 *
 * <p>Pure geometry, no libGDX: the screen passes the tube rectangles and the point to test.
 * That point must be the centre of the balls the player sees, not the finger: while dragging,
 * the run is drawn lifted above the touch point, so the finger can sit in the gap between two
 * rows (or on a tube of the other row) while the balls are plainly over the intended tube.</p>
 *
 * <p>Each tube's catch zone is its body, grown sideways by {@code sideSlack} (so the gaps
 * between neighbours belong to someone) and upwards by {@code topSlack} (so balls hovering over
 * the mouth, where lifted balls are drawn, still count). Where zones overlap, the tube whose
 * centre is nearest wins.</p>
 */
public final class DropTarget {

    private DropTarget() {
    }

    /**
     * @param px        x of the point to test (centre of the dragged balls), stage units
     * @param py        y of that point
     * @param x         left edge of each tube
     * @param y         bottom edge of each tube
     * @param width     width of each tube
     * @param height    height of each tube
     * @param sideSlack extra catch width on each side of a tube
     * @param topSlack  extra catch height above a tube's mouth
     * @return the index of the tube under the point, or -1 when it is over no tube
     */
    public static int pick(float px, float py, float[] x, float[] y, float[] width,
                           float[] height, float sideSlack, float topSlack) {
        if (x.length != y.length || x.length != width.length || x.length != height.length) {
            throw new IllegalArgumentException("tube arrays must have the same length");
        }
        int best = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < x.length; i++) {
            boolean inside = px >= x[i] - sideSlack && px <= x[i] + width[i] + sideSlack
                && py >= y[i] && py <= y[i] + height[i] + topSlack;
            if (!inside) {
                continue;
            }
            float dx = px - (x[i] + width[i] * 0.5f);
            float dy = py - (y[i] + height[i] * 0.5f);
            float distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }
}
