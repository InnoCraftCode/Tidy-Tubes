package com.ben.laama.sortpuzzle.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Packed tube encoding")
class PackedTest {

    @Test
    @DisplayName("an empty tube reports zero of everything")
    void emptyTube() {
        assertEquals(0, Packed.size(Packed.EMPTY));
        assertEquals(0, Packed.topRun(Packed.EMPTY));
        assertEquals(Packed.NO_COLOR, Packed.top(Packed.EMPTY));
        assertEquals(Packed.CAPACITY, Packed.freeSpace(Packed.EMPTY));
        assertTrue(Packed.isEmpty(Packed.EMPTY));
        assertTrue(Packed.isUniform(Packed.EMPTY));
        assertFalse(Packed.isFull(Packed.EMPTY));
        assertFalse(Packed.isComplete(Packed.EMPTY));
    }

    @Test
    @DisplayName("balls stack from the bottom and read back in order")
    void pushReadsBackInOrder() {
        int tube = Packed.EMPTY;
        tube = Packed.push(tube, 2, 1);
        tube = Packed.push(tube, 5, 2);
        tube = Packed.push(tube, 0, 1);

        assertEquals(4, Packed.size(tube));
        assertEquals(2, Packed.slot(tube, 0));
        assertEquals(5, Packed.slot(tube, 1));
        assertEquals(5, Packed.slot(tube, 2));
        assertEquals(0, Packed.slot(tube, 3));
        assertEquals(0, Packed.top(tube));
        assertEquals(1, Packed.topRun(tube));
        assertTrue(Packed.isFull(tube));
        assertFalse(Packed.isUniform(tube));
    }

    @Test
    @DisplayName("every colour index survives a round trip, including the highest")
    void allColorsRoundTrip() {
        for (int color = 0; color < 10; color++) {
            int tube = Packed.push(Packed.EMPTY, color, Packed.CAPACITY);
            assertEquals(color, Packed.top(tube), "colour " + color);
            assertEquals(Packed.CAPACITY, Packed.size(tube));
            assertTrue(Packed.isComplete(tube));
        }
    }

    @Test
    @DisplayName("the top run stops at the first different colour")
    void topRunCountsOnlyTheTopColour() {
        int tube = Tube.of(1, 3, 3, 3).encoded();
        assertEquals(3, Packed.topRun(tube));
        assertFalse(Packed.isUniform(tube));

        assertEquals(Packed.CAPACITY, Packed.topRun(Tube.full(7).encoded()));
        assertEquals(1, Packed.topRun(Tube.of(3, 3, 3, 1).encoded()));
    }

    @Test
    @DisplayName("pop is the exact inverse of push")
    void popUndoesPush() {
        int original = Tube.of(4, 4, 1).encoded();
        int pushed = Packed.push(original, 6, 1);
        assertEquals(original, Packed.pop(pushed, 1));
        assertEquals(Packed.EMPTY, Packed.pop(original, 3));
    }

    @Test
    @DisplayName("overfilling and over-popping are rejected rather than silently wrapping")
    void boundsAreEnforced() {
        int nearlyFull = Tube.of(1, 1, 1).encoded();
        assertThrows(IllegalArgumentException.class, () -> Packed.push(nearlyFull, 2, 2));
        assertThrows(IllegalArgumentException.class, () -> Packed.pop(nearlyFull, 4));
        assertThrows(IllegalArgumentException.class, () -> Packed.push(Packed.EMPTY, 15, 1));
    }

    @Test
    @DisplayName("a full single-coloured tube is complete, a full mixed one is not")
    void completeness() {
        assertTrue(Packed.isComplete(Tube.full(3).encoded()));
        assertFalse(Packed.isComplete(Tube.of(3, 3, 3, 2).encoded()));
        assertFalse(Packed.isComplete(Tube.of(3, 3, 3).encoded()));
    }
}
