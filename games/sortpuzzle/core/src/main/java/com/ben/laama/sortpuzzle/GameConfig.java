package com.ben.laama.sortpuzzle;

import com.ben.laama.sortpuzzle.i18n.T;

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
    public static final String TAGLINE = T.k("Sort. Breathe. Repeat.");
    public static final int RELAXING_MUSIC_TRACK_COUNT = 10;
    /** Shown on the About card. Kept in sync with {@code build.gradle.kts} by hand. */
    public static final String VERSION_NAME = "1.3.0";
    /** Store page, used in the share text. */
    public static final String STORE_URL =
        "https://play.google.com/store/apps/details?id=com.ben.laama.sortpuzzle";
    /** A guided breath is offered after every Nth campaign level (first clear only). */
    public static final int BREATHE_EVERY_N_LEVELS = 5;
    /**
     * Public privacy policy URL: the root {@code index.html} of the Tidy-Tubes GitHub repository,
     * served by GitHub Pages ("main" branch, root folder). The same page also lives under
     * {@code privacy-policy/}. The About card shows the button only when this is non-empty.
     */
    public static final String PRIVACY_POLICY_URL = "https://innocraftcode.github.io/Tidy-Tubes/";

    // ---------------------------------------------------------------- viewport
    /** Minimum virtual width. The viewport extends beyond it on wider screens. */
    public static final float WORLD_WIDTH = 720f;
    /** Minimum virtual height. The viewport extends beyond it on taller (20:9) screens. */
    public static final float WORLD_HEIGHT = 1280f;
    /**
     * Upper bounds for the extension. Height: a 21:9 phone still looks designed. Width: a
     * landscape tablet, foldable or desktop window (Android 16 rotates freely above 600 dp)
     * fills its width instead of showing pillars; screens centre their content, so the extra
     * width is just backdrop.
     */
    public static final float WORLD_MAX_WIDTH = 2200f;
    public static final float WORLD_MAX_HEIGHT = 1720f;
    /**
     * Landscape minimum (phone turned sideways, tablet, unfolded foldable). 800 units of height
     * keep text at about 90 % of its portrait size on a 2400x1080 phone and at exactly the
     * portrait size on a 16:10 tablet, instead of shrinking the portrait column to fit.
     */
    public static final float WORLD_LANDSCAPE_WIDTH = 1280f;
    public static final float WORLD_LANDSCAPE_HEIGHT = 800f;
    public static final float WORLD_LANDSCAPE_MAX_WIDTH = 2400f;
    public static final float WORLD_LANDSCAPE_MAX_HEIGHT = 1100f;

    // ------------------------------------------------------------------- rules
    /** Balls per tube. */
    public static final int TUBE_CAPACITY = 4;
    /** Hard ceiling on distinct colours (bounded by the palette and by the 4-bit state encoding). */
    public static final int MAX_COLORS = 10;

    // -------------------------------------------------------------- difficulty
    /**
     * Star gates: every Nth chapter opens only once the player holds enough campaign stars
     * (daily stars never count): STAR_GATE_STARS_PER_LEVEL for each level before the chapter.
     */
    public static final int STAR_GATE_EVERY_CHAPTERS = 10;
    public static final int STAR_GATE_STARS_PER_LEVEL = 2;
    /** The campaign ends here: one million generated levels. */
    public static final int MAX_LEVEL = 1_000_000;
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
    /**
     * The daily puzzle is a hard one: its profile is drawn from the hard band (levels 51-150:
     * 8 to 10 colours) and its move limit is tighter than the campaign's.
     */
    public static final int DAILY_FIRST_LEVEL = 51;
    public static final int DAILY_MOVE_LIMIT_BASE_SLACK = 3;
    public static final float DAILY_MOVE_LIMIT_SLACK_RATIO = 0.4f;
    /** Coin price of the extra moves when no rewarded video is available. */
    public static final int EXTRA_MOVES_PRICE = 60;

    // --------------------------------------------------------------------- ads
    /** Natural breaks skipped before interstitials are considered in a session. */
    public static final int INTERSTITIAL_INITIAL_BREAKS_TO_SKIP = 0;
    /** An interstitial may show at most once every N completed levels (natural breaks). */
    public static final int INTERSTITIAL_EVERY_N_LEVELS = 2;
    /** And never sooner than this after the previous full-screen ad, rewarded included. */
    public static final long INTERSTITIAL_MIN_INTERVAL_MS = 90_000L;
    /**
     * No interstitial until this many campaign levels are cleared: a new player's first minutes
     * decide day-1 retention, which is worth far more than one early interstitial.
     */
    public static final int INTERSTITIAL_MIN_CLEARED = 5;
    /** Shop "free coins" rewarded video: coins per confirmed view, and views per local day. */
    public static final int AD_COINS_REWARD = 30;
    public static final int AD_COINS_PER_DAY = 5;
    /** Coins for the optional once-per-UTC-day rewarded bonus. */
    public static final int DAILY_REWARDED_BONUS_COINS = 10;
    /** Minimum time between confirmed rewarded extra-tube bonuses. */
    public static final long REWARDED_TUBE_COOLDOWN_MS = 90_000L;
    public static final int REWARDED_TUBE_BONUS = 1;

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
    /** Coins for the first win of a UTC daily challenge. */
    public static final int DAILY_CHALLENGE_COINS = 35;
    /** Consecutive daily wins required to unlock the alternate ball palette. */
    public static final int DAILY_PALETTE_STREAK = 3;
    public static final int STAR_PALETTE_UNLOCK = 30;
    public static final int WEEKLY_GOAL_REWARD = 30;
    public static final int ACHIEVEMENT_STAR_PALETTE = 30;
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
