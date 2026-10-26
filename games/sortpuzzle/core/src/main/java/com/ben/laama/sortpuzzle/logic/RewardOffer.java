package com.ben.laama.sortpuzzle.logic;

/**
 * What the game may offer a player who wants a hint unlock or extra moves.
 *
 * <p>Pure decision logic, kept out of the screens so it can be tested: a rewarded video when
 * the ad provider has one ready, otherwise the coin price when the player can afford it,
 * otherwise nothing. A build without ads (or a no-fill moment) therefore degrades to coins,
 * which {@code RewardOfferTest} pins down.</p>
 */
public final class RewardOffer {

    /** How the reward would be paid for. */
    public enum Kind {
        /** Watch a rewarded video. Only when an ad network reports one ready. */
        VIDEO,
        /** Spend coins. */
        COINS,
        /** Nothing can be offered right now. */
        NONE
    }

    private final Kind kind;
    private final int price;

    private RewardOffer(Kind kind, int price) {
        this.kind = kind;
        this.price = price;
    }

    /**
     * @param rewardedReady whether the ad provider has a rewarded video ready to show
     * @param coins         the player's purse
     * @param price         coin price of the reward
     */
    public static RewardOffer decide(boolean rewardedReady, int coins, int price) {
        if (rewardedReady) {
            return new RewardOffer(Kind.VIDEO, 0);
        }
        if (price > 0 && coins >= price) {
            return new RewardOffer(Kind.COINS, price);
        }
        return new RewardOffer(Kind.NONE, price);
    }

    public Kind kind() {
        return kind;
    }

    /** @return the coin price (also set on {@link Kind#NONE}, so the UI can say what it would cost) */
    public int price() {
        return price;
    }

    public boolean isAvailable() {
        return kind != Kind.NONE;
    }
}
