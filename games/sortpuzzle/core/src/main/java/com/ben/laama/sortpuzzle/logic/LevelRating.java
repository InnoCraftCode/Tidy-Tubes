package com.ben.laama.sortpuzzle.logic;

/** Move-based medal rating, independent of rendering and persistence. */
public final class LevelRating {

    private LevelRating() {
    }

    /**
     * Gives three stars at par, two for a close solve and one for any other valid win.
     * Unknown par still awards one star rather than withholding a reward.
     */
    public static int starsFor(int moves, int parMoves) {
        if (moves < 1) {
            throw new IllegalArgumentException("moves must be positive");
        }
        if (parMoves <= 0) {
            return 1;
        }
        if (moves <= parMoves) {
            return 3;
        }
        int closeSolveAllowance = Math.max(2, Math.round(parMoves * 0.25f));
        return moves <= parMoves + closeSolveAllowance ? 2 : 1;
    }
}
