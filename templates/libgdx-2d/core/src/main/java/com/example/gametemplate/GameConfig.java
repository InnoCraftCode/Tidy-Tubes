package com.example.gametemplate;

/** Every tuning value in one place. Gameplay code reads these; nothing else hard-codes numbers. */
public final class GameConfig {

    private GameConfig() {
    }

    // ---------------------------------------------------------------- display
    /** Virtual resolution. A FitViewport letterboxes it onto any phone. */
    public static final float WORLD_WIDTH = 720f;
    public static final float WORLD_HEIGHT = 1280f;

    // ---------------------------------------------------------------- round
    public static final float ROUND_SECONDS = 30f;
    public static final int MAX_TARGETS = 4;
    public static final float SPAWN_INTERVAL = 0.6f;
    public static final float TARGET_LIFETIME = 1.6f;
    public static final float TARGET_RADIUS = 70f;
    /** Keeps targets clear of the HUD strip at the top. */
    public static final float HUD_HEIGHT = 160f;

    // ---------------------------------------------------------------- ads
    /** Seconds granted by the optional "continue" rewarded ad. Offered once per round. */
    public static final float REWARD_EXTRA_SECONDS = 10f;
    /** An interstitial may follow every Nth finished round, never the first. */
    public static final int INTERSTITIAL_EVERY_N_ROUNDS = 3;
    /** ...and never less than this long after the previous one. */
    public static final long INTERSTITIAL_MIN_INTERVAL_MS = 120_000L;
}
