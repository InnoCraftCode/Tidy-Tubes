package com.ben.laama.sortpuzzle.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Progression rules")
class ProgressionRulesTest {

    @Test
    @DisplayName("weekly goal weeks begin on Monday in UTC")
    void weeklyKeyUsesMonday() {
        assertEquals("2026-11-02", WeeklyGoals.weekKey(LocalDate.of(2026, 11, 5)));
        assertEquals("2026-11-02", WeeklyGoals.weekKey(LocalDate.of(2026, 11, 2)));
        assertEquals("2026-10-26", WeeklyGoals.weekKey(LocalDate.of(2026, 11, 1)));
    }

    @Test
    @DisplayName("player-selected challenge rules can be combined and summarized")
    void challengeRulesCompose() {
        ChallengeRules classic = ChallengeRules.CLASSIC;
        assertTrue(classic.isClassic());
        assertEquals("Classic", classic.summary());
        ChallengeRules custom = new ChallengeRules(true, false, true, true);
        assertFalse(custom.isClassic());
        assertEquals("No undo  •  No boosters  •  Par limit", custom.summary());
    }

    @Test
    @DisplayName("achievement progress uses persistent player milestones")
    void achievementsReadMilestoneProgress() {
        assertEquals(7, Achievement.TEN_CLEARS.progress(7, 2, 14, 1));
        assertEquals(2, Achievement.PAR_SOLVER.progress(7, 2, 14, 1));
        assertEquals(14, Achievement.STAR_COLLECTOR.progress(7, 2, 14, 1));
        assertEquals(1, Achievement.CHALLENGE_WINNER.progress(7, 2, 14, 1));
    }
}
