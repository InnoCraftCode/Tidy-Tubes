package com.ben.laama.sortpuzzle.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsEventsTest {

    @Test
    @DisplayName("every declared event and parameter name respects the provider's rules")
    void declaredNamesAreValid() {
        for (String event : AnalyticsEvents.EVENTS) {
            assertTrue(AnalyticsEvents.isValidName(event), event);
        }
        for (String param : AnalyticsEvents.PARAMS) {
            assertTrue(AnalyticsEvents.isValidName(param), param);
        }
    }

    @Test
    @DisplayName("reserved prefixes, bad characters and long names are rejected")
    void invalidNames() {
        assertFalse(AnalyticsEvents.isValidName("firebase_level"));
        assertFalse(AnalyticsEvents.isValidName("google_x"));
        assertFalse(AnalyticsEvents.isValidName("ga_session"));
        assertFalse(AnalyticsEvents.isValidName("1level"));
        assertFalse(AnalyticsEvents.isValidName("level-start"));
        assertFalse(AnalyticsEvents.isValidName("a".repeat(41)));
        assertFalse(AnalyticsEvents.isValidName(""));
        assertFalse(AnalyticsEvents.isValidName(null));
        assertTrue(AnalyticsEvents.isValidName("a".repeat(40)));
    }

    @Test
    @DisplayName("parameter lists must alternate valid names and supported values")
    void paramLists() {
        assertTrue(AnalyticsEvents.isValidParams());
        assertTrue(AnalyticsEvents.isValidParams(AnalyticsEvents.P_LEVEL, 12,
            AnalyticsEvents.P_PERFECT, true, AnalyticsEvents.P_MODE, "daily"));
        assertFalse(AnalyticsEvents.isValidParams(AnalyticsEvents.P_LEVEL)); // odd length
        assertFalse(AnalyticsEvents.isValidParams(7, 12));                    // key not a string
        assertFalse(AnalyticsEvents.isValidParams("bad-name", 1));
        assertFalse(AnalyticsEvents.isValidParams(AnalyticsEvents.P_MODE, new Object()));
        assertFalse(AnalyticsEvents.isValidParams(AnalyticsEvents.P_MODE, "x".repeat(101)));
    }
}
