package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.GameConfig;

/**
 * The difficulty curve, as a pure function of the level index.
 *
 * <pre>
 *   levels  1 - 10 :  3-4 colours, 2 empty tubes   (learning the verbs)
 *   levels 11 - 50 :  5-7 colours, 2 empty tubes   (the long middle)
 *   levels 51 +    : 8-10 colours, 1 empty tube    (the real thing)
 * </pre>
 *
 * <p>Shuffle depth grows with both the colour count and the level index, so difficulty keeps
 * creeping up inside each band instead of stepping only at the boundaries.</p>
 */
public final class Difficulty {

    private Difficulty() {
    }

    /** @param level 1-based level index */
    public static int colorsFor(int level) {
        requireValid(level);
        if (level <= GameConfig.EASY_LAST_LEVEL) {
            // 1-4 -> 3, 5+ -> 4
            return Math.min(4, 3 + (level - 1) / 4);
        }
        if (level <= GameConfig.MEDIUM_LAST_LEVEL) {
            // 11-24 -> 5, 25-38 -> 6, 39-50 -> 7
            return Math.min(7, 5 + (level - GameConfig.EASY_LAST_LEVEL - 1) / 14);
        }
        // 51-75 -> 8, 76-100 -> 9, 101+ -> 10
        return Math.min(GameConfig.MAX_COLORS,
            8 + (level - GameConfig.MEDIUM_LAST_LEVEL - 1) / 25);
    }

    /** @param level 1-based level index */
    public static int emptyTubesFor(int level) {
        requireValid(level);
        return level <= GameConfig.MEDIUM_LAST_LEVEL ? 2 : 1;
    }

    /** @param level 1-based level index */
    public static int tubeCountFor(int level) {
        return colorsFor(level) + emptyTubesFor(level);
    }

    /**
     * How many macro moves to walk away from the solved state. One macro move is a short chain
     * of reverse moves that lands on another board with every tube full — see
     * {@link LevelGenerator}.
     *
     * @param level 1-based level index
     */
    public static int shuffleDepthFor(int level) {
        requireValid(level);
        int colors = colorsFor(level);
        return Math.min(200, 10 + 6 * colors + Math.min(level, 120) / 2);
    }

    /**
     * How hard the board should be <em>for its size</em>, from 0 (easiest the generator found)
     * to 1 (hardest).
     *
     * <p>Colour count alone is far too coarse to carry a curve: it steps up only every few
     * levels, so if every board shipped at maximum scramble, levels 1 to 4 would be four
     * equally brutal three-colour puzzles and the player would feel nothing change. So each
     * <em>colour group</em> gets its own ramp — a new colour arrives on a forgiving board and
     * tightens level by level until the next colour appears. Something moves on every single
     * level, which is the whole point.</p>
     *
     * <p>The boundaries below mirror {@link #colorsFor} exactly; {@code DifficultyTest} asserts
     * they stay in step. Past level 100 the game is at its ceiling — ten colours, one spare
     * tube — so instead of pretending to climb further, the ramp cycles, which keeps the
     * variety without the dishonesty.</p>
     *
     * @param level 1-based level index
     */
    public static float scrambleRatioFor(int level) {
        requireValid(level);
        if (level <= 4) return ramp(level - 1, 4);              // 3 colours
        if (level <= GameConfig.EASY_LAST_LEVEL) return ramp(level - 5, 6);   // 4 colours
        if (level <= 24) return ramp(level - 11, 14);           // 5 colours
        if (level <= 38) return ramp(level - 25, 14);           // 6 colours
        if (level <= GameConfig.MEDIUM_LAST_LEVEL) return ramp(level - 39, 12); // 7 colours
        if (level <= 75) return ramp(level - 51, 25);           // 8 colours
        if (level <= 100) return ramp(level - 76, 25);          // 9 colours
        return ramp((level - 101) % TAIL_CYCLE, TAIL_CYCLE);    // 10 colours, endlessly
    }

    /** The easiest board a group may ship, as a percentile. Below this a level feels like a gift. */
    private static final float SCRAMBLE_FLOOR = 0.35f;
    /** Length of the repeating ramp once the difficulty curve has run out of dials. */
    private static final int TAIL_CYCLE = 10;

    private static float ramp(int index, int span) {
        float position = span <= 1 ? 1f : index / (float) (span - 1);
        return SCRAMBLE_FLOOR + (1f - SCRAMBLE_FLOOR) * position;
    }

    /**
     * How many moves the player gets before the level is lost.
     *
     * <p>Par plus slack, rather than a flat allowance: a fixed number is a free pass on level 1
     * and a wall on level 200. The slack is generous — the point is to punish flailing, not to
     * demand the optimal line, which almost nobody finds.</p>
     *
     * @param parMoves the level's optimal move count
     * @return the move limit, or {@code 0} when par is unknown and no limit should apply
     */
    public static int moveLimitFor(int parMoves) {
        if (parMoves <= 0) {
            return 0;
        }
        int slack = Math.max(GameConfig.MOVE_LIMIT_BASE_SLACK,
            Math.round(parMoves * GameConfig.MOVE_LIMIT_SLACK_RATIO));
        return parMoves + slack;
    }

    /** Human-facing band name, used by the level-select header. */
    public static String bandName(int level) {
        requireValid(level);
        if (level <= GameConfig.EASY_LAST_LEVEL) {
            return "WARM UP";
        }
        if (level <= GameConfig.MEDIUM_LAST_LEVEL) {
            return "STEADY";
        }
        return "HARD";
    }

    private static void requireValid(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("levels are 1-based, got " + level);
        }
    }
}
