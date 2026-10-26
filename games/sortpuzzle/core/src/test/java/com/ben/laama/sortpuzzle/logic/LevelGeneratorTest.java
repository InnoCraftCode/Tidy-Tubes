package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.model.BoardState;
import com.ben.laama.sortpuzzle.model.Move;
import com.ben.laama.sortpuzzle.model.Packed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LevelGenerator")
class LevelGeneratorTest {

    /** How many levels the solvability sweep covers. */
    private static final int SWEEP = 100;

    @Test
    @DisplayName("the first 100 levels are all solvable, and the solutions really solve them")
    void everyGeneratedLevelIsSolvable() {
        for (int level = 1; level <= SWEEP; level++) {
            BoardState board = LevelGenerator.generate(level).start();
            Solver.Result result = Solver.findAny(board, GameConfig.ANY_SOLUTION_NODE_BUDGET);

            assertTrue(result.solved(), "level " + level + " is not solvable: " + result);
            assertTrue(replayWins(board, result.moves()),
                "level " + level + " produced a solution that does not solve it");
        }
    }

    @Test
    @DisplayName("a level index always yields the same board")
    void generationIsDeterministic() {
        for (int level = 1; level <= SWEEP; level++) {
            assertEquals(LevelGenerator.generate(level).start(),
                LevelGenerator.generate(level).start(),
                "level " + level + " changed between calls");
        }
    }

    @Test
    @DisplayName("the same seed reshuffles a board the same way")
    void shuffleIsReproducible() {
        BoardState solved = BoardState.solved(5, 2);
        BoardState first = LevelGenerator.shuffle(solved, 20, new Random(12345L));
        BoardState second = LevelGenerator.shuffle(solved, 20, new Random(12345L));
        BoardState other = LevelGenerator.shuffle(solved, 20, new Random(999L));

        assertEquals(first, second);
        assertFalse(first.isSolved());
        assertFalse(other.isSolved());
    }

    @Test
    @DisplayName("neighbouring levels do not produce the same board")
    void neighbouringLevelsDiffer() {
        for (int level = 1; level < SWEEP; level++) {
            BoardState here = LevelGenerator.generate(level).start();
            BoardState next = LevelGenerator.generate(level + 1).start();
            if (here.tubeCount() == next.tubeCount()) {
                assertFalse(here.equals(next), "levels " + level + " and " + (level + 1)
                    + " are the same board");
            }
        }
    }

    @Test
    @DisplayName("every colour appears exactly four times and the tube count matches the curve")
    void ballsAreConserved() {
        for (int level = 1; level <= SWEEP; level++) {
            BoardState board = LevelGenerator.generate(level).start();
            int colors = Difficulty.colorsFor(level);

            assertEquals(Difficulty.tubeCountFor(level), board.tubeCount(), "level " + level);
            assertEquals(colors, board.colorCount(), "level " + level);

            int[] tally = new int[GameConfig.MAX_COLORS];
            for (int tube = 0; tube < board.tubeCount(); tube++) {
                int encoded = board.encodedAt(tube);
                for (int slot = 0; slot < Packed.size(encoded); slot++) {
                    tally[Packed.slot(encoded, slot)]++;
                }
            }
            for (int color = 0; color < colors; color++) {
                assertEquals(Packed.CAPACITY, tally[color],
                    "level " + level + ", colour " + color);
            }
        }
    }

    @Test
    @DisplayName("levels open with full colour tubes and genuinely empty spares")
    void boardsArePresentable() {
        for (int level = 1; level <= SWEEP; level++) {
            BoardState board = LevelGenerator.generate(level).start();

            assertEquals(Difficulty.emptyTubesFor(level), board.emptyTubeCount(),
                "level " + level + " spare tubes");
            for (int tube = 0; tube < board.tubeCount(); tube++) {
                int encoded = board.encodedAt(tube);
                assertTrue(encoded == Packed.EMPTY || Packed.isFull(encoded),
                    "level " + level + " tube " + tube + " is half full");
            }
        }
    }

    @Test
    @DisplayName("no level starts solved, stuck, or with a colour already finished")
    void boardsGiveNothingAway() {
        for (int level = 1; level <= SWEEP; level++) {
            BoardState board = LevelGenerator.generate(level).start();

            assertFalse(WinChecker.isWon(board), "level " + level + " starts solved");
            assertFalse(WinChecker.isDeadlocked(board), "level " + level + " starts stuck");
            assertEquals(0, board.completeTubeCount(),
                "level " + level + " starts with a finished tube");
        }
    }

    @Test
    @DisplayName("par is a real move count and matches an optimal search")
    void parIsComputed() {
        for (int level = 1; level <= 20; level++) {
            Level withPar = LevelGenerator.generateWithPar(level);

            assertEquals(LevelGenerator.generate(level).start(), withPar.start(),
                "par computation changed the board");
            assertTrue(withPar.parMoves() > 0, "level " + level + " has no par");

            Solver.Result optimal = Solver.findOptimal(withPar.start(),
                GameConfig.PAR_SEARCH_NODE_BUDGET);
            assertTrue(optimal.solved());
            assertEquals(optimal.moveCount(), withPar.parMoves(), "level " + level);
        }
    }

    @Test
    @DisplayName("difficulty trends upwards across the bands")
    void parGrowsWithDifficulty() {
        int easy = averagePar(1, 8);
        int hard = averagePar(30, 37);

        assertTrue(hard > easy, "par did not grow: early " + easy + ", later " + hard);
    }

    @Test
    @DisplayName("the seed is stable and spreads neighbouring levels apart")
    void seedsAreStableAndDistinct() {
        assertEquals(LevelGenerator.seedFor(42), LevelGenerator.seedFor(42));
        assertFalse(LevelGenerator.seedFor(42) == LevelGenerator.seedFor(43));
    }

    private static int averagePar(int from, int to) {
        int total = 0;
        for (int level = from; level <= to; level++) {
            total += LevelGenerator.generateWithPar(level).parMoves();
        }
        return total / (to - from + 1);
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
