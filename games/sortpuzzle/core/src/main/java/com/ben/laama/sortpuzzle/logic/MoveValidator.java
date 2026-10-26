package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.model.BoardState;
import com.ben.laama.sortpuzzle.model.Move;
import com.ben.laama.sortpuzzle.model.Packed;

import java.util.ArrayList;
import java.util.List;

/**
 * The one place that decides whether a tap-tap is a legal move.
 *
 * <h2>The rule</h2>
 * <p>Balls may leave tube {@code from} when it is not empty, and may land on tube {@code to}
 * when {@code to} is not full and is either empty or already shows the same colour on top.
 * The <em>whole</em> contiguous run of that colour travels together; if the destination cannot
 * take all of it, as many balls as fit are moved.</p>
 *
 * <p>Pure static functions over {@link BoardState}, plus an allocation-free
 * {@link #transferCount(int, int)} on raw packed tubes for the solver's inner loop.</p>
 */
public final class MoveValidator {

    private MoveValidator() {
    }

    /**
     * Core rule, expressed on packed tubes.
     *
     * @return how many balls would move, or {@code 0} when the move is illegal
     */
    public static int transferCount(int fromEncoded, int toEncoded) {
        if (fromEncoded == Packed.EMPTY) {
            return 0;
        }
        int free = Packed.freeSpace(toEncoded);
        if (free == 0) {
            return 0;
        }
        int color = Packed.top(fromEncoded);
        if (toEncoded != Packed.EMPTY && Packed.top(toEncoded) != color) {
            return 0;
        }
        return Math.min(Packed.topRun(fromEncoded), free);
    }

    /** @return {@code true} when tapping {@code from} then {@code to} would move something */
    public static boolean canMove(BoardState state, int from, int to) {
        if (from == to || from < 0 || to < 0
            || from >= state.tubeCount() || to >= state.tubeCount()) {
            return false;
        }
        return transferCount(state.encodedAt(from), state.encodedAt(to)) > 0;
    }

    /**
     * @return the move that tapping {@code from} then {@code to} produces, or {@code null}
     *         when that is not legal
     */
    public static Move moveFor(BoardState state, int from, int to) {
        if (from == to || from < 0 || to < 0
            || from >= state.tubeCount() || to >= state.tubeCount()) {
            return null;
        }
        int fromEncoded = state.encodedAt(from);
        int count = transferCount(fromEncoded, state.encodedAt(to));
        if (count == 0) {
            return null;
        }
        return new Move(from, to, count, Packed.top(fromEncoded));
    }

    /** @return every legal move in the position. Allocates — not for the solver's hot path. */
    public static List<Move> legalMoves(BoardState state) {
        List<Move> moves = new ArrayList<>();
        int tubes = state.tubeCount();
        for (int from = 0; from < tubes; from++) {
            int fromEncoded = state.encodedAt(from);
            if (fromEncoded == Packed.EMPTY) {
                continue;
            }
            for (int to = 0; to < tubes; to++) {
                if (to == from) {
                    continue;
                }
                int count = transferCount(fromEncoded, state.encodedAt(to));
                if (count > 0) {
                    moves.add(new Move(from, to, count, Packed.top(fromEncoded)));
                }
            }
        }
        return moves;
    }

    public static boolean hasLegalMove(BoardState state) {
        int tubes = state.tubeCount();
        for (int from = 0; from < tubes; from++) {
            int fromEncoded = state.encodedAt(from);
            if (fromEncoded == Packed.EMPTY) {
                continue;
            }
            for (int to = 0; to < tubes; to++) {
                if (to != from && transferCount(fromEncoded, state.encodedAt(to)) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A move is "useful" when it can plausibly help. Shuffling a finished tube around, or
     * tipping a single-coloured tube into an empty one, only churns the position — the solver
     * skips these and so does the hint system.
     */
    public static boolean isUseful(BoardState state, int from, int to) {
        int fromEncoded = state.encodedAt(from);
        int toEncoded = state.encodedAt(to);
        if (transferCount(fromEncoded, toEncoded) == 0) {
            return false;
        }
        if (Packed.isComplete(fromEncoded)) {
            return false; // never take balls out of a finished tube
        }
        // Emptying a single-coloured tube into an empty one just renames the tube.
        return !(toEncoded == Packed.EMPTY && Packed.isUniform(fromEncoded));
    }
}
