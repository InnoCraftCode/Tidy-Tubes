package com.ben.laama.sortpuzzle.ui;

import com.ben.laama.sortpuzzle.GameConfig;

/**
 * Viewport sizing per orientation, as pure arithmetic (unit-tested; no libGDX types).
 *
 * <p>Portrait keeps the 720x1280 design. Landscape gets its own 1280x800 minimum instead of
 * squeezing the portrait column into the short side, which used to shrink everything to about
 * half size on a phone turned sideways. Screens ask {@code BaseScreen.isLandscape()} and lay
 * themselves out in columns when it is true.</p>
 */
public final class Responsive {

    private Responsive() {
    }

    /** Square and wider counts as landscape: an unfolded foldable gets the two-column layouts. */
    public static boolean isLandscape(int screenWidth, int screenHeight) {
        return screenWidth >= screenHeight;
    }

    public static float minWorldWidth(boolean landscape) {
        return landscape ? GameConfig.WORLD_LANDSCAPE_WIDTH : GameConfig.WORLD_WIDTH;
    }

    public static float minWorldHeight(boolean landscape) {
        return landscape ? GameConfig.WORLD_LANDSCAPE_HEIGHT : GameConfig.WORLD_HEIGHT;
    }

    public static float maxWorldWidth(boolean landscape) {
        return landscape ? GameConfig.WORLD_LANDSCAPE_MAX_WIDTH : GameConfig.WORLD_MAX_WIDTH;
    }

    public static float maxWorldHeight(boolean landscape) {
        return landscape ? GameConfig.WORLD_LANDSCAPE_MAX_HEIGHT : GameConfig.WORLD_MAX_HEIGHT;
    }

    /** Pixels per world unit the ExtendViewport will use on a screen of this size. */
    public static float pixelsPerUnit(int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return 1f;
        }
        boolean landscape = isLandscape(screenWidth, screenHeight);
        return Math.min(screenWidth / minWorldWidth(landscape),
            screenHeight / minWorldHeight(landscape));
    }

    /**
     * Font rasterisation density: the larger of the two orientations' scales, so text stays
     * crisp after a rotation (the skin is built once), clamped to keep glyph pages small.
     */
    public static float fontDensity(int screenWidth, int screenHeight) {
        float density = Math.max(pixelsPerUnit(screenWidth, screenHeight),
            pixelsPerUnit(screenHeight, screenWidth));
        return Math.max(1f, Math.min(2.5f, density));
    }
}
