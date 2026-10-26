package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.GameConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Difficulty curve")
class DifficultyTest {

    @Test
    @DisplayName("levels 1-10 use 3 or 4 colours and two spare tubes")
    void earlyBand() {
        for (int level = 1; level <= 10; level++) {
            int colors = Difficulty.colorsFor(level);
            assertTrue(colors >= 3 && colors <= 4, "level " + level + " had " + colors);
            assertEquals(2, Difficulty.emptyTubesFor(level));
        }
    }

    @Test
    @DisplayName("levels 11-50 use 5 to 7 colours and two spare tubes")
    void middleBand() {
        for (int level = 11; level <= 50; level++) {
            int colors = Difficulty.colorsFor(level);
            assertTrue(colors >= 5 && colors <= 7, "level " + level + " had " + colors);
            assertEquals(2, Difficulty.emptyTubesFor(level));
        }
    }

    @Test
    @DisplayName("levels 51 and up use 8 to 10 colours and a single spare tube")
    void lateBand() {
        for (int level = 51; level <= 400; level++) {
            int colors = Difficulty.colorsFor(level);
            assertTrue(colors >= 8 && colors <= GameConfig.MAX_COLORS,
                "level " + level + " had " + colors);
            assertEquals(1, Difficulty.emptyTubesFor(level));
        }
    }

    @Test
    @DisplayName("colour count never goes backwards")
    void monotonic() {
        int previous = 0;
        for (int level = 1; level <= 500; level++) {
            int colors = Difficulty.colorsFor(level);
            assertTrue(colors >= previous, "level " + level + " dropped from " + previous);
            previous = colors;
        }
    }

    @Test
    @DisplayName("tube count is colours plus spares, and shuffle depth stays bounded")
    void derivedValues() {
        for (int level = 1; level <= 400; level++) {
            assertEquals(Difficulty.colorsFor(level) + Difficulty.emptyTubesFor(level),
                Difficulty.tubeCountFor(level));
            int depth = Difficulty.shuffleDepthFor(level);
            assertTrue(depth > 0 && depth <= 200, "level " + level + " depth " + depth);
        }
    }

    @Test
    @DisplayName("the scramble ramp climbs within a colour group and restarts at each new one")
    void scrambleRampTracksColourGroups() {
        // The boundaries in scrambleRatioFor are written out by hand and must mirror colorsFor.
        // This is the test that catches them drifting apart.
        for (int level = 2; level <= 100; level++) {
            boolean newGroup = Difficulty.colorsFor(level) != Difficulty.colorsFor(level - 1)
                || Difficulty.emptyTubesFor(level) != Difficulty.emptyTubesFor(level - 1);
            float here = Difficulty.scrambleRatioFor(level);
            float previous = Difficulty.scrambleRatioFor(level - 1);

            if (newGroup) {
                assertTrue(here < previous,
                    "level " + level + " adds a colour but the ramp did not restart");
            } else {
                assertTrue(here >= previous,
                    "level " + level + " keeps its colours but the ramp went backwards");
            }
        }
    }

    @Test
    @DisplayName("the ramp stays in range and cycles once the curve runs out of dials")
    void scrambleRampBounds() {
        for (int level = 1; level <= 400; level++) {
            float ratio = Difficulty.scrambleRatioFor(level);
            assertTrue(ratio >= 0f && ratio <= 1f, "level " + level + " ratio " + ratio);
        }
        assertEquals(1f, Difficulty.scrambleRatioFor(100), 1e-6);
        // Past level 100 nothing else can grow, so the ramp repeats rather than flat-lining.
        assertEquals(Difficulty.scrambleRatioFor(101), Difficulty.scrambleRatioFor(111), 1e-6);
        assertTrue(Difficulty.scrambleRatioFor(110) > Difficulty.scrambleRatioFor(101));
    }

    @Test
    @DisplayName("the move limit scales with par and always leaves real slack")
    void moveLimit() {
        assertEquals(0, Difficulty.moveLimitFor(0), "unknown par means no limit");
        assertEquals(0, Difficulty.moveLimitFor(-3));

        for (int par = 1; par <= 60; par++) {
            int limit = Difficulty.moveLimitFor(par);
            assertTrue(limit >= par + GameConfig.MOVE_LIMIT_BASE_SLACK,
                "par " + par + " got limit " + limit);
            assertTrue(limit > Difficulty.moveLimitFor(par - 1) || par == 1,
                "the limit must grow with par, broke at " + par);
        }
        // Small puzzles lean on the flat slack, big ones on the proportional part.
        assertEquals(7 + 6, Difficulty.moveLimitFor(7));
        assertEquals(30 + 24, Difficulty.moveLimitFor(30));
    }

    @Test
    @DisplayName("level indices are 1-based")
    void zeroIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Difficulty.colorsFor(0));
        assertThrows(IllegalArgumentException.class, () -> Difficulty.emptyTubesFor(-3));
    }
}
