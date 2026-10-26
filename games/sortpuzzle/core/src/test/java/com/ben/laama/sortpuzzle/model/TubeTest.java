package com.ben.laama.sortpuzzle.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tube")
class TubeTest {

    @Test
    @DisplayName("push and pop return new tubes and leave the original untouched")
    void immutability() {
        Tube original = Tube.of(1, 1);
        Tube pushed = original.push(2, 1);
        Tube popped = original.popTop(1);

        assertEquals(2, original.size());
        assertEquals(3, pushed.size());
        assertEquals(1, popped.size());
        assertNotSame(original, pushed);
    }

    @Test
    @DisplayName("the empty tube is a shared instance")
    void emptyIsInterned() {
        assertSame(Tube.empty(), Tube.of());
        assertSame(Tube.empty(), Tube.of(3).popTop(1));
        assertTrue(Tube.empty().isEmpty());
    }

    @Test
    @DisplayName("tubes with the same contents are equal and hash alike")
    void valueSemantics() {
        assertEquals(Tube.of(2, 2, 5), Tube.of(2, 2, 5));
        assertEquals(Tube.of(2, 2, 5).hashCode(), Tube.of(2, 2, 5).hashCode());
        assertNotEquals(Tube.of(2, 5), Tube.of(5, 2), "order matters");
    }

    @Test
    @DisplayName("balls() reads bottom first")
    void ballsAreBottomFirst() {
        List<Ball> balls = Tube.of(0, 4, 4).balls();
        assertEquals(3, balls.size());
        assertEquals(Ball.of(0), balls.get(0));
        assertEquals(Ball.of(4), balls.get(2));
        assertSame(Ball.of(4), balls.get(1), "balls are interned");
    }

    @Test
    @DisplayName("a tube cannot hold more than its capacity")
    void capacityIsEnforced() {
        assertThrows(IllegalArgumentException.class, () -> Tube.of(1, 1, 1, 1, 1));
    }

    @Test
    @DisplayName("uniform, full and complete mean what they say")
    void predicates() {
        assertTrue(Tube.full(6).isComplete());
        assertTrue(Tube.full(6).isUniform());
        assertTrue(Tube.of(6, 6).isUniform());
        assertFalse(Tube.of(6, 6).isComplete());
        assertFalse(Tube.of(6, 6, 6, 1).isUniform());
        assertEquals(2, Tube.of(6, 6).freeSpace());
        assertEquals(6, Tube.of(6, 6).topColor());
        assertEquals(Packed.NO_COLOR, Tube.empty().topColor());
    }
}
