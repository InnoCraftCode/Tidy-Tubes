package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;

/**
 * Every colour the game uses: the "Nocturne" design system (see {@code docs/design-system.md}).
 *
 * <p>Ball colours are chosen to stay distinguishable for the most common forms of colour-vision
 * deficiency: they differ in lightness as well as in hue, and each ball also carries a distinct
 * glyph-free specular shape in {@link BallActor}.</p>
 *
 * <p>The {@link Color} instances are shared. Read them, pass them to
 * {@code setColor(...)}, but never mutate them in place.</p>
 */
public final class Palette {

    private Palette() {
    }

    // ---------------------------------------------------------- backdrop
    public static final Color BG_TOP = Color.valueOf("222B5E");
    public static final Color BG_BOTTOM = Color.valueOf("0B0E24");
    /** Soft colour washes drifting behind everything. */
    public static final Color AURORA_A = Color.valueOf("3FB7C9");
    public static final Color AURORA_B = Color.valueOf("7B5CE6");

    // ---------------------------------------------------------- surfaces
    /** Translucent card / tube body. */
    public static final Color SURFACE = Color.valueOf("FFFFFF14");
    public static final Color SURFACE_STRONG = Color.valueOf("FFFFFF26");
    /** Fine light edge on cards and tubes. */
    public static final Color EDGE = Color.valueOf("FFFFFF52");
    /** Opaque card used for dialogs, so the board never bleeds through the text. */
    public static final Color CARD = Color.valueOf("1C2250F7");
    public static final Color SCRIM = Color.valueOf("05071599");
    public static final Color SHADOW = Color.valueOf("00000070");

    // ------------------------------------------------------------ accents
    /** Primary action: mint. */
    public static final Color PRIMARY = Color.valueOf("4FE3C1");
    public static final Color PRIMARY_DOWN = Color.valueOf("2FB89A");
    /** Text sitting on a primary button. */
    public static final Color ON_PRIMARY = Color.valueOf("05221B");
    /** Coins, boosters, praise. */
    public static final Color GOLD = Color.valueOf("FFC857");
    public static final Color GOLD_DOWN = Color.valueOf("D9A33A");
    public static final Color ON_GOLD = Color.valueOf("2A1D00");
    public static final Color SUCCESS = Color.valueOf("5BE49B");
    public static final Color DANGER = Color.valueOf("FF6F7D");
    public static final Color INFO = Color.valueOf("7CC4FF");

    // -------------------------------------------------------------- text
    public static final Color TEXT = Color.valueOf("F4F6FF");
    public static final Color TEXT_DIM = Color.valueOf("A3AACB");
    public static final Color TEXT_DISABLED = Color.valueOf("5C6388");

    // ------------------------------------------------------------- balls
    private static final Color[] BALLS = {
        Color.valueOf("F2504B"), // 0  coral red
        Color.valueOf("3B82F6"), // 1  blue
        Color.valueOf("34C759"), // 2  green
        Color.valueOf("FFD23F"), // 3  yellow
        Color.valueOf("A855F7"), // 4  violet
        Color.valueOf("FF8A3D"), // 5  orange
        Color.valueOf("14B8A6"), // 6  teal
        Color.valueOf("F472B6"), // 7  pink
        Color.valueOf("CBD5E1"), // 8  silver
        Color.valueOf("9A5B2B"), // 9  brown
    };

    /** @return the number of distinct ball colours the palette can render. */
    public static int ballCount() {
        return BALLS.length;
    }

    /**
     * @param colorIndex 0-based colour id coming out of the model layer
     * @return the shared colour instance — do not mutate
     */
    public static Color ball(int colorIndex) {
        return BALLS[Math.floorMod(colorIndex, BALLS.length)];
    }
}
