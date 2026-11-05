package com.ben.laama.sortpuzzle.ads;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ben.laama.sortpuzzle.GameConfig;

@DisplayName("AdPacing")
class AdPacingTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);

    @Test
    @DisplayName("only every Nth break qualifies, after the first two")
    void everyNth() {
        AdPacing pacing = new AdPacing(2, 3, 0, now::get);
        assertFalse(pacing.onBreak()); // 1
        assertFalse(pacing.onBreak()); // 2
        assertTrue(pacing.onBreak());  // 3
        assertFalse(pacing.onBreak()); // 4
        assertFalse(pacing.onBreak()); // 5
        assertTrue(pacing.onBreak());  // 6
    }

    @Test
    @DisplayName("even every-break pacing skips the first two breaks")
    void neverFirst() {
        AdPacing pacing = new AdPacing(2, 1, 0, now::get);
        assertFalse(pacing.onBreak());
        assertFalse(pacing.onBreak());
        assertTrue(pacing.onBreak());
    }

    @Test
    @DisplayName("the minimum interval blocks back-to-back full-screen ads, rewarded included")
    void minInterval() {
        AdPacing pacing = new AdPacing(0, 1, 120_000, now::get);
        pacing.onBreak();
        pacing.onFullScreenAdShown(); // e.g. the player watched a rewarded ad
        now.addAndGet(30_000);
        assertFalse(pacing.onBreak());
        now.addAndGet(90_000);
        assertTrue(pacing.onBreak());
    }

    @Test
    @DisplayName("the shipped caps skip two breaks and keep interstitials 120 s apart")
    void shippedCaps() {
        assertEquals(2, GameConfig.INTERSTITIAL_INITIAL_BREAKS_TO_SKIP);
        assertTrue(GameConfig.INTERSTITIAL_EVERY_N_LEVELS >= 3);
        assertEquals(120_000L, GameConfig.INTERSTITIAL_MIN_INTERVAL_MS);
    }

    @Test
    @DisplayName("a non-positive frequency is rejected")
    void rejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> new AdPacing(0, 0, 0, now::get));
    }

    @Test
    @DisplayName("a negative number of initial breaks is rejected")
    void rejectsNegativeInitialBreaks() {
        assertThrows(IllegalArgumentException.class, () -> new AdPacing(-1, 3, 0, now::get));
    }
}
