package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.model.Move;

/**
 * What the game has to say about the position in front of the player.
 *
 * <p>Immutable, and deliberately explicit about how much it knows: a hint that cannot prove its
 * move count says so rather than printing a number the player would take as gospel.</p>
 */
public final class Hint {

    /** What the advice actually is. */
    public enum Kind {
        /** Play {@link Hint#move()}. */
        MOVE,
        /** The position can no longer be solved — the only way forward is backwards. */
        UNDO,
        /** Nothing left to do. */
        ALREADY_WON,
        /** The search ran out of budget. No advice, and no claim that there is none. */
        UNKNOWN
    }

    private final Kind kind;
    private final Move move;
    private final int remainingMoves;
    private final boolean exact;

    private Hint(Kind kind, Move move, int remainingMoves, boolean exact) {
        this.kind = kind;
        this.move = move;
        this.remainingMoves = remainingMoves;
        this.exact = exact;
    }

    static Hint move(Move move, int remainingMoves, boolean exact) {
        return new Hint(Kind.MOVE, move, remainingMoves, exact);
    }

    static Hint undo() {
        return new Hint(Kind.UNDO, null, 0, false);
    }

    static Hint alreadyWon() {
        return new Hint(Kind.ALREADY_WON, null, 0, true);
    }

    static Hint unknown() {
        return new Hint(Kind.UNKNOWN, null, 0, false);
    }

    public Kind kind() {
        return kind;
    }

    /** @return the move to play, or {@code null} for every kind but {@link Kind#MOVE} */
    public Move move() {
        return move;
    }

    /**
     * @return how many moves still separate the player from the win, or {@code 0} when unknown.
     *         Read it together with {@link #isExact()}.
     */
    public int remainingMoves() {
        return remainingMoves;
    }

    /**
     * @return {@code true} when {@link #remainingMoves()} is provably the shortest path, and
     *         {@code false} when it is merely a route that works — an upper bound
     */
    public boolean isExact() {
        return exact;
    }

    public boolean hasMove() {
        return kind == Kind.MOVE;
    }

    @Override
    public String toString() {
        switch (kind) {
            case MOVE:
                return "play " + move + (exact ? ", " : ", at most ") + remainingMoves + " to go";
            case UNDO:
                return "position is lost, undo";
            case ALREADY_WON:
                return "already won";
            default:
                return "no advice within budget";
        }
    }
}
