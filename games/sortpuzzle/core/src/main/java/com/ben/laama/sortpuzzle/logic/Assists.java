package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.GameConfig;

/**
 * The three in-level assists and what they cost: extra tubes, undos and resets. Each level
 * starts with a free allowance; past it the player draws on a stock bought with coins or
 * earned from a rewarded video (product decision 2026-11-04: assists are no longer unlimited).
 *
 * <p>Pure: the per-level counters live in the screen, the stock in the save, the prices here.</p>
 */
public enum Assists {

    TUBE("tube", "Extra tube", GameConfig.FREE_BOOSTERS_PER_LEVEL, 1, GameConfig.BOOSTER_PRICE, 1),
    UNDO("undo", "Undo", 3, 5, 30, 5),
    RESET("reset", "Reset", 1, 3, 30, 3);

    private final String id;
    private final String title;
    private final int freePerLevel;
    private final int packSize;
    private final int packPrice;
    private final int videoReward;

    Assists(String id, String title, int freePerLevel, int packSize, int packPrice, int videoReward) {
        this.id = id;
        this.title = title;
        this.freePerLevel = freePerLevel;
        this.packSize = packSize;
        this.packPrice = packPrice;
        this.videoReward = videoReward;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    /** Free uses at the start of every level (and after a reset). */
    public int freePerLevel() {
        return freePerLevel;
    }

    /** How many a coin purchase adds. */
    public int packSize() {
        return packSize;
    }

    public int packPrice() {
        return packPrice;
    }

    /** How many a rewarded video adds. */
    public int videoReward() {
        return videoReward;
    }

    /** "5 undos" / "1 extra tube" - what one pack or one video gives. */
    public String packLabel() {
        return packSize + " " + plural(packSize);
    }

    public String videoLabel() {
        return videoReward + " " + plural(videoReward);
    }

    public String plural(int count) {
        switch (this) {
            case TUBE:
                return count == 1 ? "extra tube" : "extra tubes";
            case UNDO:
                return count == 1 ? "undo" : "undos";
            default:
                return count == 1 ? "reset" : "resets";
        }
    }

    /**
     * Takes one use: the free allowance first, then the stock.
     *
     * @param freeLeft free uses left this level
     * @param stock    bought/earned uses in the save
     * @return {@code 0} when nothing was available, {@code 1} when a free use was taken,
     *         {@code 2} when a stocked one was
     */
    public static int take(int freeLeft, int stock) {
        if (freeLeft > 0) return 1;
        if (stock > 0) return 2;
        return 0;
    }
}
