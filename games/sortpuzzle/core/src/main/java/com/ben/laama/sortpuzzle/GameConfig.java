package com.ben.laama.sortpuzzle;

/**
 * Central, immutable tuning values for the whole game.
 *
 * <p>Pure Java on purpose: it is referenced from {@code model}/{@code logic} as well as from
 * the rendering layer, and those packages must stay free of libGDX imports.</p>
 */
public final class GameConfig {

    private GameConfig() {
    }

    // ------------------------------------------------------------------- brand
    /** Display name shown in-game. The Android launcher label lives in {@code strings.xml}. */
    public static final String APP_NAME = "Tidy Tubes";
    public static final String TAGLINE = "Sort. Breathe. Repeat.";
    /** Shown on the About card. Kept in sync with {@code build.gradle.kts} by hand. */
    public static final String VERSION_NAME = "1.0.0";
    /**
     * Public privacy policy URL. Empty until the page is online: the About card hides the
     * button rather than linking nowhere.
     */
    public static final String PRIVACY_POLICY_URL = "";

    // ---------------------------------------------------------------- viewport
    /** Minimum virtual width. The viewport extends beyond it on wider screens. */
    public static final float WORLD_WIDTH = 720f;
    /** Minimum virtual height. The viewport extends beyond it on taller (20:9) screens. */
    public static final float WORLD_HEIGHT = 1280f;
    /** Upper bounds for the extension, so a 4:3 tablet or a 21:9 phone still looks designed. */
    public static final float WORLD_MAX_WIDTH = 1000f;
    public static final float WORLD_MAX_HEIGHT = 1720f;

    // ------------------------------------------------------------------- rules
    /** Balls per tube. */
    public static final int TUBE_CAPACITY = 4;
    /** Hard ceiling on distinct colours (bounded by the palette and by the 4-bit state encoding). */
    public static final int MAX_COLORS = 10;

    // -------------------------------------------------------------- difficulty
    public static final int EASY_LAST_LEVEL = 10;
    public static final int MEDIUM_LAST_LEVEL = 50;

    /** Base seed mixed with the level index so a level index always yields the same board. */
    public static final long LEVEL_SEED_BASE = 0x5DEECE66DL ^ 0x9E3779B97F4A7C15L;

    // --------------------------------------------------------------- move limit
    /**
     * A level's move limit is its par plus slack, so the allowance scales with the puzzle
     * instead of being a flat number that is generous on level 1 and impossible on level 200.
     */
    public static final int MOVE_LIMIT_BASE_SLACK = 6;
    public static final float MOVE_LIMIT_SLACK_RATIO = 0.8f;
    /** Moves granted by one "extra moves" reward when the player runs out. */
    public static final int EXTRA_MOVES_PER_AD = 5;
    /** Coin price of the extra moves when no rewarded video is available. */
    public static final int EXTRA_MOVES_PRICE = 60;

    // --------------------------------------------------------------------- ads
    /** An interstitial may show at most once every N completed levels (natural breaks). */
    public static final int INTERSTITIAL_EVERY_N_LEVELS = 3;
    /** And never sooner than this after the previous full-screen ad, rewarded included. */
    public static final long INTERSTITIAL_MIN_INTERVAL_MS = 90_000L;

    // --------------------------------------------------------------------- hints
    /** Hints given without asking for anything, per level. */
    public static final int FREE_HINTS_PER_LEVEL = 1;
    /** Coin price to unlock hints for the rest of the level when no rewarded video is available. */
    public static final int HINT_UNLOCK_PRICE = 40;
    /** Node budget for the exact hint search. Falls back to any-solution when spent. */
    public static final int HINT_OPTIMAL_NODE_BUDGET = 250_000;
    /** How many times the suggested move replays before the indicator fades. */
    public static final int HINT_REPLAYS = 3;

    // ------------------------------------------------------------------ economy
    public static final int COINS_PER_WIN = 25;
    /** Extra coins when the level is finished in par moves or fewer. */
    public static final int COINS_PAR_BONUS = 15;
    /** "Extra tube" boosters granted for free on every level. */
    public static final int FREE_BOOSTERS_PER_LEVEL = 3;
    /** Price of one permanent extra-tube booster. */
    public static final int BOOSTER_PRICE = 120;

    // -------------------------------------------------------------------- feel
    /** Duration of a single ball hop, seconds. */
    public static final float BALL_MOVE_DURATION = 0.18f;
    /** Extra delay between consecutive balls of the same run. */
    public static final float BALL_MOVE_STAGGER = 0.045f;
    /** How high above the tube mouth a lifted ball floats. */
    public static final float BALL_LIFT_HEIGHT = 42f;
    /** Amplitude of the invalid-move shake. */
    public static final float SHAKE_AMPLITUDE = 14f;
    public static final float SHAKE_DURATION = 0.28f;

    // ----------------------------------------------------------------- solver
    /** Node budget for the optimal (IDA*) search used to compute par moves. */
    public static final int PAR_SEARCH_NODE_BUDGET = 400_000;
    /** Node budget for the "find any solution" search used by the solvability tests. */
    public static final int ANY_SOLUTION_NODE_BUDGET = 2_000_000;
}
