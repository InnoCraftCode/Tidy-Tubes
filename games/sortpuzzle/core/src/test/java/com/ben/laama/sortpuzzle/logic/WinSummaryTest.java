package com.ben.laama.sortpuzzle.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WinSummaryTest {

    @Test
    @DisplayName("first clear is never a record and sets the best")
    void firstClear() {
        WinSummary s = WinSummary.of(14, 12, 0);
        assertTrue(s.firstClear);
        assertFalse(s.newRecord);
        assertFalse(s.perfect);
        assertEquals(2, s.stars);
        assertEquals(0, s.previousBest);
        assertEquals(14, s.bestAfter);
    }

    @Test
    @DisplayName("beating the previous best strictly is a record")
    void newRecord() {
        WinSummary s = WinSummary.of(12, 12, 14);
        assertTrue(s.newRecord);
        assertTrue(s.perfect);
        assertEquals(3, s.stars);
        assertEquals(14, s.previousBest);
        assertEquals(12, s.bestAfter);
    }

    @Test
    @DisplayName("equalling or missing the best is not a record and keeps the old best")
    void noRecordWhenEqualOrWorse() {
        assertFalse(WinSummary.of(14, 12, 14).newRecord);
        WinSummary worse = WinSummary.of(20, 12, 14);
        assertFalse(worse.newRecord);
        assertEquals(14, worse.bestAfter);
        assertEquals(1, worse.stars);
    }

    @Test
    @DisplayName("unknown par still gives one star and is never perfect")
    void unknownPar() {
        WinSummary s = WinSummary.of(9, Level.PAR_UNKNOWN, 0);
        assertEquals(1, s.stars);
        assertFalse(s.perfect);
        assertThrows(IllegalArgumentException.class, () -> WinSummary.of(0, 5, 0));
    }

    @Test
    @DisplayName("coin sprites: none for nothing, then 3 to 12")
    void coinSprites() {
        assertEquals(0, WinSummary.coinSprites(0));
        assertEquals(3, WinSummary.coinSprites(5));
        assertEquals(3, WinSummary.coinSprites(14));
        assertEquals(5, WinSummary.coinSprites(25));
        assertEquals(8, WinSummary.coinSprites(40));
        assertEquals(12, WinSummary.coinSprites(500));
    }

    @Test
    @DisplayName("coins split across sprites always sum to the award")
    void coinsPerSpriteSumToTotal() {
        for (int earned : new int[] {5, 8, 25, 40, 73}) {
            int sprites = WinSummary.coinSprites(earned);
            int total = 0;
            for (int i = 0; i < sprites; i++) {
                total += WinSummary.coinsForSprite(earned, sprites, i);
            }
            assertEquals(earned, total, "award " + earned);
        }
        assertThrows(IllegalArgumentException.class, () -> WinSummary.coinsForSprite(10, 3, 3));
    }
}
