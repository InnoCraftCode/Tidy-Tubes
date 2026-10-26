package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.model.BoardState;
import com.ben.laama.sortpuzzle.model.Move;
import com.ben.laama.sortpuzzle.model.Tube;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Solver")
class SolverTest {

    private static final long BUDGET = 2_000_000L;

    @Test
    @DisplayName("an already solved board needs no moves")
    void solvedBoardIsZeroMoves() {
        BoardState solved = BoardState.solved(3, 2);

        assertTrue(Solver.findAny(solved, BUDGET).solved());
        assertEquals(0, Solver.findAny(solved, BUDGET).moveCount());
        assertTrue(Solver.findOptimal(solved, BUDGET).solved());
        assertEquals(0, Solver.findOptimal(solved, BUDGET).moveCount());
    }

    @Test
    @DisplayName("one obvious move is found as exactly one move")
    void trivialBoard() {
        BoardState board = BoardState.of(Tube.of(0, 0, 0), Tube.of(0), Tube.empty());
        Solver.Result optimal = Solver.findOptimal(board, BUDGET);

        assertTrue(optimal.solved());
        assertEquals(1, optimal.moveCount());
        assertTrue(replayWins(board, optimal.moves()));
    }

    @Test
    @DisplayName("the classic three-colour opening is solved in four moves, not the obvious five")
    void knownOptimum() {
        BoardState board = BoardState.of(
            Tube.of(0, 0, 0, 1),
            Tube.of(1, 1, 1, 2),
            Tube.of(2, 2, 2, 0),
            Tube.empty(),
            Tube.empty());

        // Park one ball, then let each colour cascade into the tube the previous one vacated.
        Solver.Result optimal = Solver.findOptimal(board, BUDGET);
        assertTrue(optimal.solved());
        assertEquals(4, optimal.moveCount());
        assertTrue(replayWins(board, optimal.moves()));
    }

    @Test
    @DisplayName("a board with no legal move is proven unsolvable, not merely abandoned")
    void unsolvableIsProven() {
        BoardState stuck = BoardState.of(Tube.of(0, 1, 0, 1), Tube.of(1, 0, 1, 0));

        Solver.Result any = Solver.findAny(stuck, BUDGET);
        assertFalse(any.solved());
        assertFalse(any.budgetExhausted(), "there was nothing to search, so this is a proof");

        Solver.Result optimal = Solver.findOptimal(stuck, BUDGET);
        assertFalse(optimal.solved());
        assertFalse(optimal.budgetExhausted());
    }

    @Test
    @DisplayName("a zero budget gives up rather than lying about unsolvability")
    void exhaustedBudgetIsReported() {
        BoardState board = LevelGenerator.generate(60).start();
        Solver.Result result = Solver.findAny(board, 1L);

        assertFalse(result.solved());
        assertTrue(result.budgetExhausted());
    }

    @Test
    @DisplayName("the optimal solution is never longer than the first one found")
    void optimalIsAtLeastAsGood() {
        for (int level = 1; level <= 25; level++) {
            BoardState board = LevelGenerator.generate(level).start();
            Solver.Result any = Solver.findAny(board, BUDGET);
            Solver.Result optimal = Solver.findOptimal(board, BUDGET);

            assertTrue(any.solved(), "level " + level + " unsolved by findAny");
            assertTrue(optimal.solved(), "level " + level + " unsolved by findOptimal");
            assertTrue(optimal.moveCount() <= any.moveCount(),
                "level " + level + ": optimal " + optimal.moveCount()
                    + " > any " + any.moveCount());
        }
    }

    @Test
    @DisplayName("every move in a returned solution is legal in the position it is played")
    void solutionsAreLegal() {
        for (int level = 1; level <= 40; level++) {
            BoardState board = LevelGenerator.generate(level).start();
            Solver.Result result = Solver.findAny(board, BUDGET);
            assertTrue(result.solved(), "level " + level);
            assertTrue(replayWins(board, result.moves()), "level " + level);
        }
    }

    @Test
    @DisplayName("the heuristic is zero on a win and never overestimates the real distance")
    void heuristicIsAdmissible() {
        assertEquals(0, Solver.heuristic(BoardState.solved(5, 2).toEncodedArray()));

        for (int level = 1; level <= 25; level++) {
            BoardState board = LevelGenerator.generate(level).start();
            int estimate = Solver.heuristic(board.toEncodedArray());
            Solver.Result optimal = Solver.findOptimal(board, BUDGET);

            assertTrue(optimal.solved());
            assertTrue(estimate <= optimal.moveCount(),
                "level " + level + ": heuristic " + estimate
                    + " overestimated the optimum " + optimal.moveCount());
        }
    }

    @Test
    @DisplayName("results carry a readable summary")
    void resultToString() {
        Solver.Result result = Solver.findAny(LevelGenerator.generate(1).start(), BUDGET);
        assertNotNull(result.toString());
        assertTrue(result.nodes() >= 0);
    }

    /** Replays a solution move by move, checking legality at each step. */
    private static boolean replayWins(BoardState start, List<Move> moves) {
        BoardState state = start;
        for (Move move : moves) {
            Move legal = MoveValidator.moveFor(state, move.from(), move.to());
            if (legal == null || legal.count() != move.count()
                || legal.colorIndex() != move.colorIndex()) {
                return false;
            }
            state = state.apply(move);
        }
        return WinChecker.isWon(state);
    }
}
