package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.model.BoardState;
import com.ben.laama.sortpuzzle.model.Tube;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WinChecker")
class WinCheckerTest {

    @Test
    @DisplayName("every non-empty tube full and single-coloured is a win")
    void winningBoards() {
        assertTrue(WinChecker.isWon(BoardState.of(Tube.full(0), Tube.full(1), Tube.empty())));
        assertTrue(WinChecker.isWon(BoardState.of(Tube.empty(), Tube.empty())));
        assertTrue(WinChecker.isWon(BoardState.solved(4, 2)));
    }

    @Test
    @DisplayName("a single-coloured tube that is not full is not a win")
    void partiallyFilledIsNotAWin() {
        assertFalse(WinChecker.isWon(BoardState.of(Tube.of(0, 0, 0), Tube.of(0))));
    }

    @Test
    @DisplayName("a full mixed tube is not a win")
    void mixedIsNotAWin() {
        assertFalse(WinChecker.isWon(BoardState.of(Tube.of(0, 0, 0, 1), Tube.of(1, 1, 1, 0))));
    }

    @Test
    @DisplayName("a board with no useful move left is deadlocked")
    void deadlock() {
        BoardState stuck = BoardState.of(Tube.of(0, 1, 0, 1), Tube.of(1, 0, 1, 0));
        assertTrue(WinChecker.isDeadlocked(stuck));

        BoardState playable = BoardState.of(Tube.of(0, 1, 0, 1), Tube.of(1, 0, 1), Tube.empty());
        assertFalse(WinChecker.isDeadlocked(playable));
    }

    @Test
    @DisplayName("a won board is never reported as deadlocked")
    void winBeatsDeadlock() {
        assertFalse(WinChecker.isDeadlocked(BoardState.of(Tube.full(0), Tube.full(1))));
    }

    @Test
    @DisplayName("progress counts finished colours")
    void progress() {
        BoardState board = BoardState.of(
            Tube.full(0), Tube.of(1, 1, 1, 2), Tube.of(2, 2, 2, 1));
        assertEquals(1f / 3f, WinChecker.progress(board), 1e-6);
        assertEquals(1f, WinChecker.progress(BoardState.solved(3, 1)), 1e-6);
    }
}
