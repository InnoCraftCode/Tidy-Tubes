package com.ben.laama.sortpuzzle.logic;

/**
 * What the game reports to each leaderboard, as pure functions of saved progress. Keeps the
 * "what is a score" decision out of the screens and off the platform SDK.
 */
public final class LeaderboardScores {

    private LeaderboardScores() {
    }

    /**
     * @param highestUnlocked the first level not yet cleared (1-based, as in {@code SaveData})
     * @return campaign levels completed, never negative
     */
    public static long highestLevel(int highestUnlocked) {
        return Math.max(0, highestUnlocked - 1);
    }

    /** @return total stars clamped to a sane range (3 per level, never negative) */
    public static long totalStars(int totalStars) {
        return Math.max(0, totalStars);
    }

    /**
     * Daily puzzle score: the move count itself. Configure that board as "smaller is better" in
     * Play Console; a daily board also resets every day there.
     *
     * @throws IllegalArgumentException for a non-positive move count (no win takes zero moves)
     */
    public static long dailyMoves(int moves) {
        if (moves < 1) {
            throw new IllegalArgumentException("a win takes at least one move");
        }
        return moves;
    }
}
