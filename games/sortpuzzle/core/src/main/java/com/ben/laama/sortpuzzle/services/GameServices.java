package com.ben.laama.sortpuzzle.services;

/**
 * Platform game services: sign-in and leaderboards (Google Play Games Services on Android).
 *
 * <p>Same contract as {@code AdProvider}: the core only ever talks to this interface, the
 * launcher injects the real implementation, and every other platform gets {@link NoOpGameServices}.
 * Nothing here is required to play - the game must behave identically when
 * {@link #isAvailable()} is {@code false}. Callbacks arrive on the render thread.</p>
 */
public interface GameServices {

    /** The leaderboards the game publishes. IDs are platform configuration, not game logic. */
    enum Leaderboard {
        /** Highest campaign level completed; larger is better. */
        HIGHEST_LEVEL,
        /** Total stars across all campaign levels; larger is better. */
        TOTAL_STARS,
        /** Moves used on today's daily puzzle; smaller is better (configure the board that way). */
        DAILY_FEWEST_MOVES
    }

    /** Result of a sign-in attempt. */
    interface SignInListener {
        void onResult(boolean signedIn);
    }

    /**
     * Called once from {@code Main.create()}; may start a silent sign-in. The listener fires on
     * the render thread with the outcome (immediately with {@code false} where nothing exists).
     */
    void initialize(SignInListener onSilentSignIn);

    /** @return whether this build and platform have game services configured at all */
    boolean isAvailable();

    boolean isSignedIn();

    /** @return the signed-in player's display name, or an empty string */
    String playerName();

    /** Interactive sign-in (the button). The listener always fires, on the render thread. */
    void signIn(SignInListener listener);

    /** Fire-and-forget; silently ignored when unavailable or signed out. */
    void submitScore(Leaderboard board, long score);

    /** Opens the platform UI for one board; no-op when unavailable or signed out. */
    void showLeaderboard(Leaderboard board);

    /** Opens the platform UI listing every board; no-op when unavailable or signed out. */
    void showLeaderboards();
}
