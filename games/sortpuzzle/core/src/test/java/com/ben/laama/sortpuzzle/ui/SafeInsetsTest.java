package com.ben.laama.sortpuzzle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class SafeInsetsTest {

    private static final float EPSILON = 0.001f;

    @Test
    void noCutoutMeansNoInsets() {
        SafeInsets insets = SafeInsets.toWorld(0, 0, 0, 0, 1080, 2340, 0, 0, 1080, 2340, 720f);
        assertEquals(0f, insets.top, EPSILON);
        assertEquals(0f, insets.bottom, EPSILON);
        assertEquals(0f, insets.left, EPSILON);
        assertEquals(0f, insets.right, EPSILON);
    }

    @Test
    void fullScreenViewportScalesPixelsToWorldUnits() {
        // 1080 px across 720 units = 1.5 px per unit; a 120 px camera hole is 80 units.
        SafeInsets insets = SafeInsets.toWorld(120, 0, 0, 0, 1080, 2340, 0, 0, 1080, 2340, 720f);
        assertEquals(80f, insets.top, EPSILON);
        assertEquals(0f, insets.bottom, EPSILON);
    }

    @Test
    void landscapeCutoutOnTheSideBecomesLeftInset() {
        // Tablet rotated so the hole sits on the left; 2560 px across 2048 units = 1.25 px/unit.
        SafeInsets insets = SafeInsets.toWorld(0, 0, 100, 0, 2560, 1600, 0, 0, 2560, 1600, 2048f);
        assertEquals(80f, insets.left, EPSILON);
        assertEquals(0f, insets.top, EPSILON);
    }

    @Test
    void letterboxGapAbsorbsTheInset() {
        // The viewport starts 200 px from the left; a 100 px inset never reaches it.
        SafeInsets insets = SafeInsets.toWorld(0, 0, 100, 0, 2560, 1600, 200, 0, 2160, 1600, 1000f);
        assertEquals(0f, insets.left, EPSILON);
    }

    @Test
    void onlyThePartBeyondTheGapCounts() {
        // Pillarbox of 50 px at each side, 2.16 px/unit: 100 - 50 = 50 px -> 23.148 units.
        SafeInsets insets = SafeInsets.toWorld(0, 0, 100, 100, 2560, 1600, 50, 0, 2460, 1600, 1138.888f);
        assertEquals(50f / (2460f / 1138.888f), insets.left, EPSILON);
        assertEquals(insets.left, insets.right, EPSILON);
    }

    @Test
    void degenerateViewportIsSafe() {
        assertSame(SafeInsets.NONE, SafeInsets.toWorld(10, 10, 10, 10, 0, 0, 0, 0, 0, 0, 720f));
        assertSame(SafeInsets.NONE, SafeInsets.toWorld(10, 10, 10, 10, 1080, 2340, 0, 0, 1080, 2340, 0f));
    }
}
