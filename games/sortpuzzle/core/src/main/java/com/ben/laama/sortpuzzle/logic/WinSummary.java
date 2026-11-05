package com.ben.laama.sortpuzzle.logic;

/**
 * What the result screen says about a win, computed once from plain numbers so the screen only
 * animates it. Independent of rendering and persistence.
 */
public final class WinSummary {

    /** Most coin sprites the result screen flies to the purse for one award. */
    public static final int MAX_COIN_SPRITES = 12;
    /** Fewest coin sprites flown for any non-zero award. */
    public static final int MIN_COIN_SPRITES = 3;
    /** Roughly one sprite per this many coins. */
    public static final int COINS_PER_SPRITE = 5;

    /** Stars earned by this run alone (not the best ever). */
    public final int stars;
    /** Solved in the optimal number of moves. */
    public final boolean perfect;
    /** Never cleared before. */
    public final boolean firstClear;
    /** Cleared before and beaten strictly this time. */
    public final boolean newRecord;
    /** Best move count before this run, {@code 0} when none. */
    public final int previousBest;
    /** Best move count after this run. */
    public final int bestAfter;

    private WinSummary(int stars, boolean perfect, boolean firstClear, boolean newRecord,
                       int previousBest, int bestAfter) {
        this.stars = stars;
        this.perfect = perfect;
        this.firstClear = firstClear;
        this.newRecord = newRecord;
        this.previousBest = previousBest;
        this.bestAfter = bestAfter;
    }

    /**
     * @param moves             moves used in this run, positive
     * @param parMoves          optimal count, or {@code 0} / {@link Level#PAR_UNKNOWN} when unknown
     * @param previousBestMoves best count recorded before this run, {@code 0} when never cleared
     */
    public static WinSummary of(int moves, int parMoves, int previousBestMoves) {
        if (moves < 1) {
            throw new IllegalArgumentException("moves must be positive");
        }
        int stars = LevelRating.starsFor(moves, parMoves);
        boolean perfect = parMoves > 0 && moves <= parMoves;
        boolean firstClear = previousBestMoves <= 0;
        boolean newRecord = !firstClear && moves < previousBestMoves;
        int bestAfter = firstClear ? moves : Math.min(moves, previousBestMoves);
        return new WinSummary(stars, perfect, firstClear, newRecord,
            Math.max(0, previousBestMoves), bestAfter);
    }

    /** @return how many coin sprites to fly for {@code coinsEarned}: 0 for nothing, else 3..12 */
    public static int coinSprites(int coinsEarned) {
        if (coinsEarned <= 0) {
            return 0;
        }
        int sprites = (coinsEarned + COINS_PER_SPRITE - 1) / COINS_PER_SPRITE;
        return Math.max(MIN_COIN_SPRITES, Math.min(MAX_COIN_SPRITES, sprites));
    }

    /**
     * Splits an award across its sprites so the purse counter ends exactly on the total.
     *
     * @return the coins the {@code index}-th landing sprite adds, remainder on the last one
     */
    public static int coinsForSprite(int coinsEarned, int spriteCount, int index) {
        if (spriteCount <= 0 || index < 0 || index >= spriteCount) {
            throw new IllegalArgumentException("bad sprite index " + index + "/" + spriteCount);
        }
        int each = coinsEarned / spriteCount;
        return index == spriteCount - 1 ? coinsEarned - each * (spriteCount - 1) : each;
    }
}
