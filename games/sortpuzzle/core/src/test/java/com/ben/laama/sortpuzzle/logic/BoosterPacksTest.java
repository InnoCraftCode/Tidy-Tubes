package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.save.SaveData;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Booster packs")
class BoosterPacksTest {

    @Test
    @DisplayName("packs are cheaper per tube than singles, and say so")
    void savings() {
        assertEquals(0, BoosterPacks.SINGLE.savingsPercent());
        assertEquals(GameConfig.BOOSTER_PRICE, BoosterPacks.SINGLE.price());
        for (BoosterPacks pack : BoosterPacks.values()) {
            float perTube = pack.price() / (float) pack.count();
            assertTrue(perTube <= GameConfig.BOOSTER_PRICE, pack.name());
        }
        assertTrue(BoosterPacks.FIVE.savingsPercent() > 0);
        assertTrue(BoosterPacks.TWELVE.savingsPercent() > BoosterPacks.FIVE.savingsPercent());
    }

    @Test
    @DisplayName("buying takes the coins once and adds the tubes; no coins, no tubes")
    void buying() {
        SaveData save = new SaveData();
        save.addCoins(600);
        assertTrue(BoosterPacks.FIVE.canBuy(save));
        assertTrue(BoosterPacks.FIVE.buy(save));
        assertEquals(100, save.coins);
        assertEquals(5, save.boosters);
        assertFalse(BoosterPacks.FIVE.canBuy(save));
        assertFalse(BoosterPacks.FIVE.buy(save));
        assertEquals(100, save.coins);
        assertEquals(5, save.boosters);
    }
}
