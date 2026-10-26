package com.ben.laama.sortpuzzle.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BoardState")
class BoardStateTest {

    @Test
    @DisplayName("the solved board is one full tube per colour plus the spares")
    void solvedLayout() {
        BoardState board = BoardState.solved(3, 2);

        assertEquals(5, board.tubeCount());
        assertEquals(3, board.colorCount());
        assertEquals(2, board.emptyTubeCount());
        assertEquals(3, board.completeTubeCount());
        assertTrue(board.isSolved());
        for (int i = 0; i < 3; i++) {
            assertEquals(i, board.tube(i).topColor());
            assertTrue(board.tube(i).isComplete());
        }
    }

    @Test
    @DisplayName("applying a move leaves the original board alone")
    void applyIsImmutable() {
        BoardState before = BoardState.of(Tube.of(1, 1), Tube.empty());
        BoardState after = before.apply(new Move(0, 1, 2, 1));

        assertEquals(2, before.tube(0).size());
        assertEquals(0, before.tube(1).size());
        assertEquals(0, after.tube(0).size());
        assertEquals(2, after.tube(1).size());
        assertNotEquals(before, after);
    }

    @Test
    @DisplayName("the booster adds an empty tube without touching the rest")
    void extraTube() {
        BoardState before = BoardState.of(Tube.of(1, 1), Tube.of(2));
        BoardState after = before.withExtraTube();

        assertEquals(3, after.tubeCount());
        assertTrue(after.tube(2).isEmpty());
        assertEquals(before.tube(0), after.tube(0));
        assertEquals(before.tube(1), after.tube(1));
        assertEquals(2, before.tubeCount(), "the original is unchanged");
    }

    @Test
    @DisplayName("boards differing only by a permutation of tubes share a canonical key")
    void canonicalKeyIgnoresTubeOrder() {
        BoardState left = BoardState.of(Tube.of(1, 1), Tube.of(2), Tube.empty());
        BoardState right = BoardState.of(Tube.empty(), Tube.of(2), Tube.of(1, 1));

        assertArrayEquals(left.canonicalKey(), right.canonicalKey());
        assertNotEquals(left, right, "equality itself is still order sensitive");
    }

    @Test
    @DisplayName("a board is solved when every non-empty tube is finished")
    void solvedDetection() {
        assertTrue(BoardState.of(Tube.full(0), Tube.empty()).isSolved());
        assertFalse(BoardState.of(Tube.of(0, 0, 0), Tube.of(0)).isSolved());
        assertFalse(BoardState.of(Tube.of(0, 0, 0, 1), Tube.of(1, 1, 1, 0)).isSolved());
    }

    @Test
    @DisplayName("toEncodedArray hands out a copy")
    void encodedArrayIsDefensive() {
        BoardState board = BoardState.of(Tube.of(1), Tube.empty());
        int[] copy = board.toEncodedArray();
        Arrays.fill(copy, 0);
        assertEquals(1, board.tube(0).size());
    }
}
