package com.ben.laama.sortpuzzle.ui;

/**
 * Display-cutout insets converted from screen pixels into world units, for a viewport that may
 * not cover the whole screen (an ExtendViewport letterboxes once a side reaches its maximum).
 *
 * <p>Pure arithmetic, so it has a unit test; {@code BaseScreen} feeds it the libGDX values and
 * shrinks its safe-area group accordingly. With {@code LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS} the
 * backdrop still paints under the camera hole, only the UI steps aside.</p>
 */
public final class SafeInsets {

    public static final SafeInsets NONE = new SafeInsets(0f, 0f, 0f, 0f);

    public final float top;
    public final float bottom;
    public final float left;
    public final float right;

    public SafeInsets(float top, float bottom, float left, float right) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
    }

    /**
     * @param insetTop       pixels the display cutout reserves at the top edge of the screen
     * @param insetBottom    same, bottom edge
     * @param insetLeft      same, left edge
     * @param insetRight     same, right edge
     * @param screenWidth    back-buffer width in pixels
     * @param screenHeight   back-buffer height in pixels
     * @param viewportX      viewport origin in pixels, bottom-left corner (GL convention)
     * @param viewportY      see {@code viewportX}
     * @param viewportWidth  viewport width in pixels
     * @param viewportHeight viewport height in pixels
     * @param worldWidth     world units spanning {@code viewportWidth}
     * @return the part of each inset that reaches into the viewport, in world units
     */
    public static SafeInsets toWorld(int insetTop, int insetBottom, int insetLeft, int insetRight,
                                     int screenWidth, int screenHeight,
                                     int viewportX, int viewportY, int viewportWidth, int viewportHeight,
                                     float worldWidth) {
        if (viewportWidth <= 0 || worldWidth <= 0f) {
            return NONE;
        }
        float pixelsPerUnit = viewportWidth / worldWidth;
        // Letterbox gaps already keep the UI away from that edge; only the remainder counts.
        float gapLeft = viewportX;
        float gapBottom = viewportY;
        float gapRight = screenWidth - viewportX - viewportWidth;
        float gapTop = screenHeight - viewportY - viewportHeight;
        return new SafeInsets(
            Math.max(0f, insetTop - gapTop) / pixelsPerUnit,
            Math.max(0f, insetBottom - gapBottom) / pixelsPerUnit,
            Math.max(0f, insetLeft - gapLeft) / pixelsPerUnit,
            Math.max(0f, insetRight - gapRight) / pixelsPerUnit);
    }

    @Override
    public String toString() {
        return "SafeInsets[top=" + top + ", bottom=" + bottom + ", left=" + left + ", right=" + right + "]";
    }
}
