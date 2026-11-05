package com.ben.laama.sortpuzzle.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeaderboardScoresTest {

    @Test
    void highestLevelIsLevelsCompleted() {
        assertEquals(0, LeaderboardScores.highestLevel(1)); // fresh save: level 1 unlocked, none done
        assertEquals(24, LeaderboardScores.highestLevel(25));
        assertEquals(0, LeaderboardScores.highestLevel(0));
    }

    @Test
    void totalStarsNeverNegative() {
        assertEquals(0, LeaderboardScores.totalStars(-3));
        assertEquals(57, LeaderboardScores.totalStars(57));
    }

    @Test
    void dailyMovesIsTheMoveCount() {
        assertEquals(14, LeaderboardScores.dailyMoves(14));
        assertThrows(IllegalArgumentException.class, () -> LeaderboardScores.dailyMoves(0));
    }
}
