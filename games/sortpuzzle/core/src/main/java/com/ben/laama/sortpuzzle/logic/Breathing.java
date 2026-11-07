package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.i18n.T;

/**
 * A short guided breath - "Sort. Breathe. Repeat." made literal. Three cycles of
 * in (4 s), hold (2 s), out (6 s): the 4-2-6 pattern lengthens the exhale, which is what
 * actually calms. Pure timing maths; the overlay only draws what this says.
 */
public final class Breathing {

    public enum Phase {
        IN(T.k("Breathe in")), HOLD(T.k("Hold")), OUT(T.k("Breathe out")), DONE("");

        private final String label;

        Phase(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public static final float IN_SECONDS = 4f;
    public static final float HOLD_SECONDS = 2f;
    public static final float OUT_SECONDS = 6f;
    public static final int CYCLES = 3;
    public static final float CYCLE_SECONDS = IN_SECONDS + HOLD_SECONDS + OUT_SECONDS;
    public static final float TOTAL_SECONDS = CYCLE_SECONDS * CYCLES;

    private Breathing() {
    }

    /** @return the phase at {@code seconds} since the start, {@link Phase#DONE} once over */
    public static Phase phaseAt(float seconds) {
        if (seconds < 0f) return Phase.IN;
        if (seconds >= TOTAL_SECONDS) return Phase.DONE;
        float t = seconds % CYCLE_SECONDS;
        if (t < IN_SECONDS) return Phase.IN;
        if (t < IN_SECONDS + HOLD_SECONDS) return Phase.HOLD;
        return Phase.OUT;
    }

    /** @return 1-based cycle number at {@code seconds}, clamped to {@link #CYCLES} */
    public static int cycleAt(float seconds) {
        if (seconds <= 0f) return 1;
        return Math.min(CYCLES, (int) (seconds / CYCLE_SECONDS) + 1);
    }

    /**
     * How full the lungs are, 0..1: rises through IN with an ease-out, stays at 1 through
     * HOLD, falls through OUT with an ease-in-out. Drives the circle's size.
     */
    public static float fullnessAt(float seconds) {
        if (seconds <= 0f) return 0f;
        if (seconds >= TOTAL_SECONDS) return 0f;
        float t = seconds % CYCLE_SECONDS;
        if (t < IN_SECONDS) {
            float p = t / IN_SECONDS;
            return 1f - (1f - p) * (1f - p);                     // ease out
        }
        if (t < IN_SECONDS + HOLD_SECONDS) {
            return 1f;
        }
        float p = (t - IN_SECONDS - HOLD_SECONDS) / OUT_SECONDS;
        float eased = p < 0.5f ? 2f * p * p : 1f - 2f * (1f - p) * (1f - p);  // ease in-out
        return 1f - eased;
    }

    /** Whether a finished campaign level is one of the "take a breath" moments. */
    public static boolean offersBreathAfter(int level, int everyNLevels) {
        return everyNLevels > 0 && level > 0 && level % everyNLevels == 0;
    }
}
