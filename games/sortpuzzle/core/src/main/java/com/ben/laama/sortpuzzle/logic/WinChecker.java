package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.model.BoardState;
import com.ben.laama.sortpuzzle.model.Packed;

/** Decides when a board is finished, and when it can no longer make progress. */
public final class WinChecker {

    private WinChecker() {
    }

    /**
     * The win condition: every non-empty tube holds exactly {@link Packed#CAPACITY} balls of a
     * single colour. Empty tubes are fine — they are the working space.
     */
    public static boolean isWon(BoardState state) {
        for (int i = 0; i < state.tubeCount(); i++) {
            int tube = state.encodedAt(i);
            if (tube != Packed.EMPTY && !Packed.isComplete(tube)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return {@code true} when the board is not won and no move can change anything useful —
     *         the player needs undo, a restart or a booster
     */
    public static boolean isDeadlocked(BoardState state) {
        if (isWon(state)) {
            return false;
        }
        int tubes = state.tubeCount();
        for (int from = 0; from < tubes; from++) {
            for (int to = 0; to < tubes; to++) {
                if (from != to && MoveValidator.isUseful(state, from, to)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** @return how far along the board is, 0..1 — handy for progress bars and scoring. */
    public static float progress(BoardState state) {
        int colors = state.colorCount();
        if (colors == 0) {
            return 1f;
        }
        return state.completeTubeCount() / (float) colors;
    }
}
