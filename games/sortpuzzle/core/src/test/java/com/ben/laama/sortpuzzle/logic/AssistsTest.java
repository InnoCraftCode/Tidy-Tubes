package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.save.SaveData;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Assists (tubes, undos, resets)")
class AssistsTest {

    @Test
    @DisplayName("free uses go first, then the stock, then nothing")
    void takeOrder() {
        assertEquals(1, Assists.take(3, 0));
        assertEquals(2, Assists.take(0, 4));
        assertEquals(0, Assists.take(0, 0));
        assertEquals(1, Assists.take(1, 9));
    }

    @Test
    @DisplayName("every assist has a free allowance, a pack and a video reward, with labels")
    void catalogue() {
        for (Assists assist : Assists.values()) {
            assertTrue(assist.freePerLevel() >= 1, assist.name());
            assertTrue(assist.packSize() >= 1 && assist.packPrice() > 0, assist.name());
            assertTrue(assist.videoReward() >= 1, assist.name());
            assertFalse(assist.packLabel().isEmpty());
        }
        assertEquals("5 undos", Assists.UNDO.packLabel());
        assertEquals("1 extra tube", Assists.TUBE.packLabel());
        assertEquals("3 resets", Assists.RESET.videoLabel());
    }

    @Test
    @DisplayName("the save keeps a stock per assist: add, consume, buy, never negative, cleared by reset")
    void saveStock() {
        SaveData save = new SaveData();
        assertEquals(0, save.stockOf(Assists.UNDO));
        save.addAssist(Assists.UNDO, 5);
        assertEquals(5, save.stockOf(Assists.UNDO));
        assertTrue(save.consumeAssist(Assists.UNDO));
        assertEquals(4, save.stockOf(Assists.UNDO));
        assertFalse(save.consumeAssist(Assists.RESET));
        assertEquals(0, save.stockOf(Assists.RESET));

        save.addCoins(100);
        assertTrue(save.buyAssistPack(Assists.RESET));
        assertEquals(100 - Assists.RESET.packPrice(), save.coins);
        assertEquals(Assists.RESET.packSize(), save.stockOf(Assists.RESET));
        save.coins = 0;
        assertFalse(save.buyAssistPack(Assists.UNDO));
        assertEquals(4, save.stockOf(Assists.UNDO));

        // Tubes share the existing booster stock.
        save.addAssist(Assists.TUBE, 2);
        assertEquals(2, save.boosters);
        assertEquals(2, save.stockOf(Assists.TUBE));
        assertTrue(save.consumeAssist(Assists.TUBE));
        assertEquals(1, save.boosters);

        save.reset();
        assertEquals(0, save.stockOf(Assists.UNDO));
        assertEquals(0, save.stockOf(Assists.RESET));
        assertEquals(0, save.stockOf(Assists.TUBE));
    }
}
