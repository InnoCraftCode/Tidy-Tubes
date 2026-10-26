package com.example.gametemplate.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.gametemplate.GameConfig;

@DisplayName("RoundState")
class RoundStateTest {

    private static final float W = GameConfig.WORLD_WIDTH;
    private static final float H = GameConfig.WORLD_HEIGHT;

    private RoundState fresh() {
        return new RoundState(new Random(42), W, H);
    }

    @Test
    @DisplayName("targets spawn inside the play area, below the HUD, and never exceed the cap")
    void spawnsInsideBounds() {
        RoundState round = fresh();
        for (int i = 0; i < 200; i++) {
            round.update(0.1f);
            assertTrue(round.targets().size() <= GameConfig.MAX_TARGETS);
            for (Target t : round.targets()) {
                assertTrue(t.x - t.radius >= 0 && t.x + t.radius <= W, "x in bounds");
                assertTrue(t.y - t.radius >= 0 && t.y + t.radius <= H - GameConfig.HUD_HEIGHT, "y in bounds");
            }
            if (round.isOver()) {
                break;
            }
        }
    }

    @Test
    @DisplayName("a tap on a target scores once and removes it; a miss scores nothing")
    void tapScores() {
        RoundState round = fresh();
        round.addTargetForTest(new Target(100, 100, 50, 10));
        assertFalse(round.tap(400, 400));
        assertTrue(round.tap(110, 95));
        assertFalse(round.tap(110, 95));
        assertEquals(1, round.score());
    }

    @Test
    @DisplayName("targets expire after their lifetime")
    void targetsExpire() {
        RoundState round = fresh();
        Target t = new Target(100, 100, 50, 0.5f);
        round.addTargetForTest(t);
        round.update(0.6f);
        assertFalse(round.targets().contains(t));
    }

    @Test
    @DisplayName("the round ends when time runs out and then ignores input")
    void roundEnds() {
        RoundState round = fresh();
        round.update(GameConfig.ROUND_SECONDS + 1);
        assertTrue(round.isOver());
        assertEquals(0f, round.timeLeft());
        assertTrue(round.targets().isEmpty());
        assertFalse(round.tap(100, 100));
    }

    @Test
    @DisplayName("the rewarded continue is available once per round and only after it ends")
    void continueOnce() {
        RoundState round = fresh();
        assertFalse(round.canContinue());
        round.update(GameConfig.ROUND_SECONDS);
        assertTrue(round.canContinue());

        round.grantContinue();
        assertFalse(round.isOver());
        assertEquals(GameConfig.REWARD_EXTRA_SECONDS, round.timeLeft());

        round.grantContinue(); // duplicate callback must not add time
        assertEquals(GameConfig.REWARD_EXTRA_SECONDS, round.timeLeft());

        round.update(GameConfig.REWARD_EXTRA_SECONDS);
        assertTrue(round.isOver());
        assertFalse(round.canContinue());
    }
}
