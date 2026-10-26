package com.example.gametemplate.ads;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AdPacing")
class AdPacingTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);

    @Test
    @DisplayName("only every Nth break qualifies, and never the first")
    void everyNth() {
        AdPacing pacing = new AdPacing(3, 0, now::get);
        assertFalse(pacing.onBreak()); // 1
        assertFalse(pacing.onBreak()); // 2
        assertTrue(pacing.onBreak());  // 3
        assertFalse(pacing.onBreak()); // 4
        assertFalse(pacing.onBreak()); // 5
        assertTrue(pacing.onBreak());  // 6
    }

    @Test
    @DisplayName("every-break pacing still skips the very first break")
    void neverFirst() {
        AdPacing pacing = new AdPacing(1, 0, now::get);
        assertFalse(pacing.onBreak());
        assertTrue(pacing.onBreak());
    }

    @Test
    @DisplayName("the minimum interval blocks back-to-back full-screen ads, rewarded included")
    void minInterval() {
        AdPacing pacing = new AdPacing(1, 60_000, now::get);
        pacing.onBreak();
        pacing.onFullScreenAdShown(); // e.g. the player watched a rewarded ad
        now.addAndGet(30_000);
        assertFalse(pacing.onBreak());
        now.addAndGet(30_000);
        assertTrue(pacing.onBreak());
    }

    @Test
    @DisplayName("a non-positive frequency is rejected")
    void rejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> new AdPacing(0, 0, now::get));
    }
}
