package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;

import com.ben.laama.sortpuzzle.logic.Cosmetic;

/**
 * Every colour the game uses: the "Nocturne" design system (see {@code docs/design-system.md}).
 *
 * <p>Ball colours are chosen to stay distinguishable for the most common forms of colour-vision
 * deficiency: they differ in lightness as well as in hue, and each ball also carries a distinct
 * glyph-free specular shape in {@link BallActor}.</p>
 *
 * <p>The {@link Color} instances are shared. Read them, pass them to {@code setColor(...)}, but
 * never mutate them in place from a screen. The only writers are the {@code apply*} methods
 * here, called by {@link Cosmetics} when the player equips a theme, a ball palette or a tube
 * style: they update the shared instances so every consumer sees the new look without being
 * told. Anything baked into a texture (the background gradient) is rebuilt by
 * {@link Cosmetics} alongside.</p>
 */
public final class Palette {

    private Palette() {
    }

    // ---------------------------------------------------------- backdrop (themed)
    public static final Color BG_TOP = Color.valueOf("222B5E");
    public static final Color BG_BOTTOM = Color.valueOf("0B0E24");
    /** Soft colour washes drifting behind everything. */
    public static final Color AURORA_A = Color.valueOf("3FB7C9");
    public static final Color AURORA_B = Color.valueOf("7B5CE6");

    // ---------------------------------------------------------- surfaces
    /** Translucent card body. */
    public static final Color SURFACE = Color.valueOf("FFFFFF14");
    public static final Color SURFACE_STRONG = Color.valueOf("FFFFFF26");
    /** Fine light edge on cards. */
    public static final Color EDGE = Color.valueOf("FFFFFF52");
    /** Opaque card used for dialogs, so the board never bleeds through the text. */
    public static final Color CARD = Color.valueOf("1C2250F7");
    public static final Color SCRIM = Color.valueOf("05071599");
    public static final Color SHADOW = Color.valueOf("00000070");

    // ------------------------------------------------------------ tubes (styled)
    /** Glass body tint, and its brighter twin for the selected tube. */
    public static final Color TUBE_BODY = new Color(SURFACE);
    public static final Color TUBE_BODY_STRONG = new Color(SURFACE_STRONG);
    /** Rim stroke colour. */
    public static final Color TUBE_RIM = new Color(EDGE);

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
    private static final int BALL_COLOURS = 10;
    private static final Color[] BALLS = new Color[BALL_COLOURS];
    private static Cosmetic ballPalette = Cosmetic.BALL_CLASSIC;
    private static Cosmetic theme = Cosmetic.THEME_NOCTURNE;
    private static Cosmetic tubeStyle = Cosmetic.TUBE_GLASS;

    static {
        for (int i = 0; i < BALL_COLOURS; i++) {
            BALLS[i] = new Color();
        }
        applyBallPalette(Cosmetic.BALL_CLASSIC);
    }

    /** @return the number of distinct ball colours the palette can render. */
    public static int ballCount() {
        return BALL_COLOURS;
    }

    /**
     * @param colorIndex 0-based colour id coming out of the model layer
     * @return the shared colour instance — do not mutate
     */
    public static Color ball(int colorIndex) {
        return BALLS[Math.floorMod(colorIndex, BALL_COLOURS)];
    }

    // ---------------------------------------------------------- cosmetics
    public static Cosmetic ballPalette() {
        return ballPalette;
    }

    public static Cosmetic theme() {
        return theme;
    }

    public static Cosmetic tubeStyle() {
        return tubeStyle;
    }

    /** Writes the palette's ten colours into the shared ball instances. */
    public static void applyBallPalette(Cosmetic palette) {
        require(palette, Cosmetic.Category.BALL_PALETTE);
        for (int i = 0; i < BALL_COLOURS; i++) {
            BALLS[i].set(Color.valueOf(palette.hex(i % palette.hexCount())));
        }
        ballPalette = palette;
    }

    /**
     * Writes the theme's backdrop colours into the shared instances. The baked gradient
     * texture is not touched here: see {@link Cosmetics#apply}.
     */
    public static void applyTheme(Cosmetic item) {
        require(item, Cosmetic.Category.THEME);
        BG_TOP.set(Color.valueOf(item.hex(0)));
        BG_BOTTOM.set(Color.valueOf(item.hex(1)));
        AURORA_A.set(Color.valueOf(item.hex(2)));
        AURORA_B.set(Color.valueOf(item.hex(3)));
        theme = item;
    }

    /** Writes the tube style's body and rim colours into the shared instances. */
    public static void applyTubeStyle(Cosmetic item) {
        require(item, Cosmetic.Category.TUBE);
        TUBE_BODY.set(Color.valueOf(item.hex(0)));
        TUBE_BODY_STRONG.set(TUBE_BODY);
        TUBE_BODY_STRONG.a = Math.min(1f, TUBE_BODY.a * 1.9f);
        TUBE_RIM.set(Color.valueOf(item.hex(1)));
        tubeStyle = item;
    }

    private static void require(Cosmetic item, Cosmetic.Category category) {
        if (item == null || item.category() != category) {
            throw new IllegalArgumentException("expected a " + category + " item, got " + item);
        }
    }
}
