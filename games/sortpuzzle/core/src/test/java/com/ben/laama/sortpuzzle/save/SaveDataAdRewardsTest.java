package com.ben.laama.sortpuzzle.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ben.laama.sortpuzzle.GameConfig;

import org.junit.jupiter.api.Test;

class SaveDataAdRewardsTest {

    @Test
    void freeCoinsVideosAreCappedPerDay() {
        SaveData save = new SaveData();
        assertEquals(GameConfig.AD_COINS_PER_DAY, save.adCoinsLeft("2026-11-05"));

        for (int i = 0; i < GameConfig.AD_COINS_PER_DAY; i++) {
            assertTrue(save.claimAdCoins("2026-11-05"));
        }

        assertFalse(save.claimAdCoins("2026-11-05"), "cap reached: nothing more today");
        assertEquals(0, save.adCoinsLeft("2026-11-05"));
        assertEquals(GameConfig.AD_COINS_PER_DAY * GameConfig.AD_COINS_REWARD, save.coins);
    }

    @Test
    void capResetsOnANewDay() {
        SaveData save = new SaveData();
        for (int i = 0; i < GameConfig.AD_COINS_PER_DAY; i++) {
            save.claimAdCoins("2026-11-05");
        }

        assertTrue(save.claimAdCoins("2026-11-06"));

        assertEquals(GameConfig.AD_COINS_PER_DAY - 1, save.adCoinsLeft("2026-11-06"));
    }

    @Test
    void emptyDateNeverCredits() {
        SaveData save = new SaveData();

        assertFalse(save.claimAdCoins(""));
        assertFalse(save.claimAdCoins(null));

        assertEquals(0, save.coins);
    }

    @Test
    void resetClearsTheDailyCount() {
        SaveData save = new SaveData();
        save.claimAdCoins("2026-11-05");

        save.reset();

        assertEquals(GameConfig.AD_COINS_PER_DAY, save.adCoinsLeft("2026-11-05"));
    }
}
