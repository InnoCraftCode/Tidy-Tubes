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
    @DisplayName("daily level profiles stay inside the first 100 balanced campaign levels")
    void difficultyProfileIsBounded() {
        LocalDate first = LocalDate.of(2026, 1, 1);
        for (int day = 0; day < 365; day++) {
            int level = DailyChallenge.levelFor(first.plusDays(day));
            assertTrue(level >= 1 && level <= 100);
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
