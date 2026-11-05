package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.save.SaveData;

/**
 * Extra-tube boosters sold in the shop, singly or by the pack. Packs cost less per tube, which
 * is the one honest reason to buy more at once. Coins only; no real money anywhere.
 */
public enum BoosterPacks {

    SINGLE(1, GameConfig.BOOSTER_PRICE, "One extra tube"),
    FIVE(5, 500, "Pack of five"),
    TWELVE(12, 1000, "Pack of twelve");

    private final int count;
    private final int price;
    private final String title;

    BoosterPacks(int count, int price, String title) {
        this.count = count;
        this.price = price;
        this.title = title;
    }

    public int count() {
        return count;
    }

    public int price() {
        return price;
    }

    public String title() {
        return title;
    }

    /** Percent saved against buying the tubes one by one, 0 for the single. */
    public int savingsPercent() {
        int single = count * GameConfig.BOOSTER_PRICE;
        return single <= price ? 0 : Math.round(100f * (single - price) / single);
    }

    public boolean canBuy(SaveData save) {
        return save.coins >= price;
    }

    /** @return {@code true} when the coins were taken and the tubes added */
    public boolean buy(SaveData save) {
        return save.buyBoosters(count, price);
    }
}
