package com.ben.laama.sortpuzzle.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReminderAskPolicy")
class ReminderAskPolicyTest {

    private static final long TODAY = 20_000L;

    @Test
    @DisplayName("not before the third session")
    void sessionGate() {
        assertFalse(ReminderAskPolicy.shouldAsk(2, 0, 0, TODAY, false, false));
        assertTrue(ReminderAskPolicy.shouldAsk(3, 0, 0, TODAY, false, false));
    }

    @Test
    @DisplayName("'Not now' waits three days")
    void reAskAfterThreeDays() {
        long next = ReminderAskPolicy.nextAskAfterDecline(TODAY);
        assertEquals(TODAY + 3, next);
        assertFalse(ReminderAskPolicy.shouldAsk(5, 1, next, TODAY + 2, false, false));
        assertTrue(ReminderAskPolicy.shouldAsk(5, 1, next, TODAY + 3, false, false));
    }

    @Test
    @DisplayName("never more than three asks")
    void maxAsks() {
        assertFalse(ReminderAskPolicy.shouldAsk(9, 3, 0, TODAY, false, false));
    }

    @Test
    @DisplayName("never after a system denial, never when already on")
    void deniedOrOn() {
        assertFalse(ReminderAskPolicy.shouldAsk(9, 0, 0, TODAY, false, true));
        assertFalse(ReminderAskPolicy.shouldAsk(9, 0, 0, TODAY, true, false));
    }
}
