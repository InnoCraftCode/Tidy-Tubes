package com.ben.laama.sortpuzzle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResponsiveLayoutTest {

    private static final float EPSILON = 0.01f;

    @Test
    void portraitPhoneKeepsTheDesignScale() {
        assertFalse(Responsive.isLandscape(1080, 2400));
        assertEquals(1.5f, Responsive.pixelsPerUnit(1080, 2400), EPSILON);
    }

    @Test
    void landscapePhoneNoLongerShrinksToHalfSize() {
        // Before: the 1280-unit height had to fit 1080 px, i.e. 0.84 px per unit.
        assertTrue(Responsive.isLandscape(2400, 1080));
        assertEquals(1.35f, Responsive.pixelsPerUnit(2400, 1080), EPSILON);
    }

    @Test
    void tabletIsTheSameScaleInBothOrientations() {
        assertEquals(2f, Responsive.pixelsPerUnit(1600, 2560), EPSILON);
        assertEquals(2f, Responsive.pixelsPerUnit(2560, 1600), EPSILON);
    }

    @Test
    void fontDensityCoversARotationAndIsClamped() {
        assertEquals(1.5f, Responsive.fontDensity(1080, 2400), EPSILON);
        assertEquals(1.5f, Responsive.fontDensity(2400, 1080), EPSILON);
        assertEquals(2.5f, Responsive.fontDensity(4000, 6000), EPSILON);
        assertEquals(1f, Responsive.fontDensity(360, 640), EPSILON);
    }

    @Test
    void portraitBoardMatchesTheOriginalRowRule() {
        // Six tubes: one row at full size; seven: two rows of four at full size.
        BoardLayout six = BoardLayout.fit(6, 768f, 910f, 108f, 16f, 70f, 7, 9f, 4);
        assertEquals(1, six.rows);
        assertEquals(108f, six.tubeWidth, EPSILON);
        BoardLayout seven = BoardLayout.fit(7, 768f, 910f, 108f, 16f, 70f, 7, 9f, 4);
        assertEquals(2, seven.rows);
        assertEquals(4, seven.perRow);
        assertEquals(108f, seven.tubeWidth, EPSILON);
    }

    @Test
    void landscapeBandPutsManyTubesOnOneRow() {
        BoardLayout layout = BoardLayout.fit(9, 1150f, 620f, 108f, 16f, 70f, 12, 9f, 4);
        assertEquals(1, layout.rows);
        assertEquals(9, layout.perRow);
    }

    @Test
    void boardAlwaysFitsItsArea() {
        for (int count = 3; count <= 16; count++) {
            for (float[] area : new float[][] {{768f, 910f}, {1150f, 620f}, {680f, 600f}, {500f, 400f}}) {
                BoardLayout layout = BoardLayout.fit(count, area[0], area[1], 108f, 16f, 70f, 12, 9f, 4);
                float width = layout.perRow * layout.tubeWidth + (layout.perRow - 1) * 16f;
                assertTrue(width <= area[0] + EPSILON, "too wide: " + count + " in " + area[0]);
                assertTrue(layout.stackHeight(70f) <= area[1] + EPSILON,
                    "too tall: " + count + " in " + area[1]);
                assertTrue(layout.rows * layout.perRow >= count);
            }
        }
    }
}
