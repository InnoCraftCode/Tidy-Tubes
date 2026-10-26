package com.ben.laama.sortpuzzle.ads;

import java.util.function.LongSupplier;

/**
 * Decides whether a natural break (level complete) may show an interstitial.
 *
 * <p>Rules: never on the first break of a session, only every Nth break, and never sooner than a
 * minimum interval after the previous full-screen ad. A rewarded ad the player chose to watch
 * also resets the interval — they just saw a full-screen ad. The clock is injected for tests.
 * The numbers live in {@code GameConfig}.</p>
 */
public final class AdPacing {

    private final int everyNBreaks;
    private final long minIntervalMs;
    private final LongSupplier clock;

    private int breaks;
    private long lastFullScreenAt = Long.MIN_VALUE;

    public AdPacing(int everyNBreaks, long minIntervalMs, LongSupplier clock) {
        if (everyNBreaks < 1) {
            throw new IllegalArgumentException("everyNBreaks must be >= 1");
        }
        this.everyNBreaks = everyNBreaks;
        this.minIntervalMs = minIntervalMs;
        this.clock = clock;
    }

    /** Call once per natural break. @return whether an interstitial may be shown now. */
    public boolean onBreak() {
        breaks++;
        if (breaks < 2 || breaks % everyNBreaks != 0) {
            return false;
        }
        return lastFullScreenAt == Long.MIN_VALUE || clock.getAsLong() - lastFullScreenAt >= minIntervalMs;
    }

    /** Call when any full-screen ad (interstitial or rewarded) was actually shown. */
    public void onFullScreenAdShown() {
        lastFullScreenAt = clock.getAsLong();
    }
}
