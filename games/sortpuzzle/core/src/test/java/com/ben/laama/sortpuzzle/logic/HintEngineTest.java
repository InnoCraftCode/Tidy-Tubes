package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.model.BoardState;
import com.ben.laama.sortpuzzle.model.Move;
import com.ben.laama.sortpuzzle.model.Tube;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HintEngine")
class HintEngineTest {

    @Test
    @DisplayName("a finished board has nothing to advise")
    void alreadyWon() {
        Hint hint = HintEngine.advise(BoardState.solved(3, 2));

        assertEquals(Hint.Kind.ALREADY_WON, hint.kind());
        assertFalse(hint.hasMove());
        assertNull(hint.move());
    }

    @Test
    @DisplayName("one move from the end, the advice is that move and the count is exact")
    void oneMoveOut() {
        BoardState board = BoardState.of(Tube.of(0, 0, 0), Tube.of(0), Tube.empty());
        Hint hint = HintEngine.advise(board);

        assertEquals(Hint.Kind.MOVE, hint.kind());
        assertEquals(1, hint.remainingMoves());
        assertTrue(hint.isExact());
        assertNotNull(hint.move());
        assertTrue(WinChecker.isWon(board.apply(hint.move())));
    }

    @Test
    @DisplayName("following the advice, move after move, actually finishes the level")
    void hintsLeadToAWin() {
        for (int level = 1; level <= 12; level++) {
            BoardState state = LevelGenerator.generate(level).start();
            int previousRemaining = Integer.MAX_VALUE;
            int played = 0;

            while (!WinChecker.isWon(state)) {
                Hint hint = HintEngine.advise(state);
                assertTrue(hint.hasMove(),
                    "level " + level + " gave no move after " + played + ": " + hint);
                assertTrue(hint.remainingMoves() > 0, "level " + level + " claimed 0 left");

                if (hint.isExact()) {
                    assertTrue(hint.remainingMoves() < previousRemaining,
                        "level " + level + " went from " + previousRemaining
                            + " to " + hint.remainingMoves() + " remaining");
                    previousRemaining = hint.remainingMoves();
                }

                Move move = hint.move();
                assertNotNull(MoveValidator.moveFor(state, move.from(), move.to()),
                    "level " + level + " advised an illegal move: " + move);
                state = state.apply(move);
                played++;

                assertTrue(played < 200, "level " + level + " is not converging");
            }
            assertTrue(played > 0);
        }
    }

    @Test
    @DisplayName("a lost position is called lost, and the advice is to undo")
    void deadPositionAdvisesUndo() {
        BoardState stuck = BoardState.of(Tube.of(0, 1, 0, 1), Tube.of(1, 0, 1, 0));
        Hint hint = HintEngine.advise(stuck);

        assertEquals(Hint.Kind.UNDO, hint.kind());
        assertFalse(hint.hasMove());
    }

    @Test
    @DisplayName("running out of budget is reported as not knowing, never as a dead end")
    void exhaustedBudgetIsNotADeadEnd() {
        BoardState board = LevelGenerator.generate(80).start();
        Hint hint = HintEngine.advise(board, 1L, 1L);

        assertEquals(Hint.Kind.UNKNOWN, hint.kind());
        assertFalse(hint.hasMove());
        assertEquals(0, hint.remainingMoves());
    }

    @Test
    @DisplayName("when only a non-optimal route is found, the count is flagged as a bound")
    void fallbackCountIsNotClaimedExact() {
        BoardState board = LevelGenerator.generate(60).start();
        // No budget for the exact pass, plenty for the fallback.
        Hint hint = HintEngine.advise(board, 1L, 2_000_000L);

        assertEquals(Hint.Kind.MOVE, hint.kind());
        assertFalse(hint.isExact(), "an any-solution count must not claim to be optimal");
        assertTrue(hint.remainingMoves() > 0);
        assertNotNull(MoveValidator.moveFor(board, hint.move().from(), hint.move().to()));
    }
}
