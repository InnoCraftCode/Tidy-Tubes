package com.example.gametemplate.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.example.gametemplate.GameConfig;

/**
 * The whole game rule set: spawn targets, age them, score taps, end the round when time runs out.
 *
 * <p>Plain Java on purpose — the screen only draws what this class says is true, and every rule is
 * covered by a unit test that runs without a GPU. The {@link Random} is injected so tests are
 * deterministic.</p>
 */
public final class RoundState {

    private final Random random;
    private final float width;
    private final float playTop;
    private final List<Target> targets = new ArrayList<>();

    private float timeLeft = GameConfig.ROUND_SECONDS;
    private float spawnTimer;
    private int score;
    private boolean continueUsed;

    public RoundState(Random random, float width, float height) {
        this.random = random;
        this.width = width;
        this.playTop = height - GameConfig.HUD_HEIGHT;
    }

    /** Advances the simulation. Does nothing once the round is over. */
    public void update(float delta) {
        if (isOver()) {
            return;
        }
        timeLeft = Math.max(0f, timeLeft - delta);
        for (int i = targets.size() - 1; i >= 0; i--) {
            Target t = targets.get(i);
            t.age(delta);
            if (t.expired()) {
                targets.remove(i);
            }
        }
        spawnTimer -= delta;
        if (spawnTimer <= 0f && targets.size() < GameConfig.MAX_TARGETS) {
            spawn();
            spawnTimer = GameConfig.SPAWN_INTERVAL;
        }
        if (isOver()) {
            targets.clear();
        }
    }

    /** @return {@code true} when the tap hit a target (which is removed and scored). */
    public boolean tap(float x, float y) {
        if (isOver()) {
            return false;
        }
        // Topmost (most recently spawned) first, so overlapping circles behave as drawn.
        for (int i = targets.size() - 1; i >= 0; i--) {
            if (targets.get(i).contains(x, y)) {
                targets.remove(i);
                score++;
                return true;
            }
        }
        return false;
    }

    /** Whether the one-per-round rewarded continue can still be offered. */
    public boolean canContinue() {
        return isOver() && !continueUsed;
    }

    /** Applies the rewarded continue. Idempotent: a second call in the same round is ignored. */
    public void grantContinue() {
        if (!canContinue()) {
            return;
        }
        continueUsed = true;
        timeLeft = GameConfig.REWARD_EXTRA_SECONDS;
        spawnTimer = 0f;
    }

    void addTargetForTest(Target t) {
        targets.add(t);
    }

    private void spawn() {
        float r = GameConfig.TARGET_RADIUS;
        float x = r + random.nextFloat() * (width - 2 * r);
        float y = r + random.nextFloat() * (playTop - 2 * r);
        targets.add(new Target(x, y, r, GameConfig.TARGET_LIFETIME));
    }

    public boolean isOver() {
        return timeLeft <= 0f;
    }

    public float timeLeft() {
        return timeLeft;
    }

    public int score() {
        return score;
    }

    public List<Target> targets() {
        return Collections.unmodifiableList(targets);
    }
}
