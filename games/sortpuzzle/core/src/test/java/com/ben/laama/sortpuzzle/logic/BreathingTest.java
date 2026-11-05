package com.ben.laama.sortpuzzle.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Breathing")
class BreathingTest {

    @Test
    @DisplayName("4-2-6 phases repeat for three cycles, then it is done")
    void phases() {
        assertEquals(Breathing.Phase.IN, Breathing.phaseAt(0f));
        assertEquals(Breathing.Phase.IN, Breathing.phaseAt(3.9f));
        assertEquals(Breathing.Phase.HOLD, Breathing.phaseAt(4f));
        assertEquals(Breathing.Phase.HOLD, Breathing.phaseAt(5.9f));
        assertEquals(Breathing.Phase.OUT, Breathing.phaseAt(6f));
        assertEquals(Breathing.Phase.OUT, Breathing.phaseAt(11.9f));
        assertEquals(Breathing.Phase.IN, Breathing.phaseAt(12f));
        assertEquals(Breathing.Phase.OUT, Breathing.phaseAt(35.9f));
        assertEquals(Breathing.Phase.DONE, Breathing.phaseAt(36f));
        assertEquals(36f, Breathing.TOTAL_SECONDS, 1e-6);
    }

    @Test
    @DisplayName("cycle counter is 1-based and clamps at the end")
    void cycles() {
        assertEquals(1, Breathing.cycleAt(0f));
        assertEquals(1, Breathing.cycleAt(11.9f));
        assertEquals(2, Breathing.cycleAt(12f));
        assertEquals(3, Breathing.cycleAt(30f));
        assertEquals(3, Breathing.cycleAt(99f));
    }

    @Test
    @DisplayName("fullness rises to 1, holds, falls back to 0, and never leaves 0..1")
    void fullness() {
        assertEquals(0f, Breathing.fullnessAt(0f), 1e-6);
        assertTrue(Breathing.fullnessAt(2f) > 0.5f);
        assertEquals(1f, Breathing.fullnessAt(4f), 1e-6);
        assertEquals(1f, Breathing.fullnessAt(5f), 1e-6);
        assertEquals(0.5f, Breathing.fullnessAt(9f), 1e-6);   // halfway through the exhale
        assertEquals(0f, Breathing.fullnessAt(12f), 1e-6);
        assertEquals(0f, Breathing.fullnessAt(36f), 1e-6);
        float previous = 0f;
        for (float t = 0f; t < 4f; t += 0.1f) {               // monotonic while breathing in
            float now = Breathing.fullnessAt(t);
            assertTrue(now >= previous - 1e-6);
            previous = now;
        }
        for (float t = 0f; t < 40f; t += 0.05f) {
            float f = Breathing.fullnessAt(t);
            assertTrue(f >= 0f && f <= 1f, "t=" + t);
        }
    }

    @Test
    @DisplayName("a breath is offered every N campaign levels")
    void offers() {
        assertTrue(Breathing.offersBreathAfter(5, 5));
        assertTrue(Breathing.offersBreathAfter(10, 5));
        assertFalse(Breathing.offersBreathAfter(4, 5));
        assertFalse(Breathing.offersBreathAfter(0, 5));
        assertFalse(Breathing.offersBreathAfter(5, 0));
    }
}
