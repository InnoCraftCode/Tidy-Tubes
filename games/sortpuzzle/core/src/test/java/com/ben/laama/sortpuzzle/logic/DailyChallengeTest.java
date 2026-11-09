package com.ben.laama.sortpuzzle.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Daily challenge")
class DailyChallengeTest {

    @Test
    @DisplayName("every date selects a shared, reproducible difficulty and board")
    void selectionIsDeterministic() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        LocalDate nextDate = date.plusDays(1);
        int level = DailyChallenge.levelFor(date);
        long seed = DailyChallenge.seedFor(date);

        assertEquals(level, DailyChallenge.levelFor(date));
        assertEquals(seed, DailyChallenge.seedFor(date));
        assertEquals(LevelGenerator.generate(level, seed).start(),
            LevelGenerator.generate(level, seed).start());
        assertNotEquals(LevelGenerator.generate(level).start(),
            LevelGenerator.generate(level, seed).start());
        assertNotEquals(LevelGenerator.generate(DailyChallenge.levelFor(nextDate),
            DailyChallenge.seedFor(nextDate)).start(),
            LevelGenerator.generate(level, seed).start());
    }

    @Test
    @DisplayName("daily level profiles always come from the hard band (51-150, 8+ colours)")
    void difficultyProfileIsHard() {
        LocalDate first = LocalDate.of(2026, 1, 1);
        for (int day = 0; day < 365; day++) {
            int level = DailyChallenge.levelFor(first.plusDays(day));
            assertTrue(level >= 51 && level <= 150, "day " + day + " -> level " + level);
            assertTrue(Difficulty.colorsFor(level) >= 8, "day " + day + " has too few colours");
        }
    }

    @Test
    @DisplayName("two weeks of hard daily boards are all solvable")
    void hardDailiesAreSolvable() {
        LocalDate first = LocalDate.of(2026, 11, 9);
        for (int day = 0; day < 14; day++) {
            LocalDate date = first.plusDays(day);
            com.ben.laama.sortpuzzle.model.BoardState board = LevelGenerator
                .generate(DailyChallenge.levelFor(date), DailyChallenge.seedFor(date)).start();

            Solver.Result result = Solver.findAny(board,
                com.ben.laama.sortpuzzle.GameConfig.ANY_SOLUTION_NODE_BUDGET);

            assertTrue(result.solved(), date + " is not solvable: " + result);
        }
    }

    @Test
    @DisplayName("stars reward par, a close solve and any other valid win")
    void moveRating() {
        assertEquals(3, LevelRating.starsFor(12, 12));
        assertEquals(3, LevelRating.starsFor(9, 12));
        assertEquals(2, LevelRating.starsFor(15, 12));
        assertEquals(1, LevelRating.starsFor(16, 12));
        assertEquals(1, LevelRating.starsFor(8, 0));
    }

    @Test
    @DisplayName("non-positive move counts cannot earn stars")
    void invalidMoveCountIsRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> LevelRating.starsFor(0, 0));
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> DailyChallenge.dateKey(null));
    }
}
