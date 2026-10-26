package com.ben.laama.sortpuzzle.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.ads.DisabledAdProvider;

class RewardOfferTest {

    @Test
    void offersCoinsWhenAffordableAndNoVideo() {
        RewardOffer offer = RewardOffer.decide(false, 100, 40);
        assertEquals(RewardOffer.Kind.COINS, offer.kind());
        assertEquals(40, offer.price());
        assertTrue(offer.isAvailable());
    }

    @Test
    void offersNothingWhenBroke() {
        RewardOffer offer = RewardOffer.decide(false, 39, 40);
        assertEquals(RewardOffer.Kind.NONE, offer.kind());
        assertEquals(40, offer.price(), "the price is still reported so the UI can explain");
        assertFalse(offer.isAvailable());
    }

    @Test
    void exactPurseIsEnough() {
        assertEquals(RewardOffer.Kind.COINS, RewardOffer.decide(false, 40, 40).kind());
    }

    @Test
    void videoWinsWhenReady() {
        RewardOffer offer = RewardOffer.decide(true, 0, 40);
        assertEquals(RewardOffer.Kind.VIDEO, offer.kind());
        assertEquals(0, offer.price());
    }

    @Test
    void zeroPriceNeverMeansFree() {
        assertEquals(RewardOffer.Kind.NONE, RewardOffer.decide(false, 1000, 0).kind());
    }

    @Test
    void aBuildWithoutAdsFallsBackToCoinsOrNothing() {
        boolean ready = new DisabledAdProvider().isRewardedReady();
        assertFalse(ready);
        for (int coins : new int[] {0, GameConfig.HINT_UNLOCK_PRICE, 10_000}) {
            RewardOffer hints = RewardOffer.decide(ready, coins, GameConfig.HINT_UNLOCK_PRICE);
            RewardOffer moves = RewardOffer.decide(ready, coins, GameConfig.EXTRA_MOVES_PRICE);
            assertTrue(hints.kind() != RewardOffer.Kind.VIDEO);
            assertTrue(moves.kind() != RewardOffer.Kind.VIDEO);
        }
    }
}
