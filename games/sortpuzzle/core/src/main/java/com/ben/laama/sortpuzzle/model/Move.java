package com.ben.laama.sortpuzzle.model;

/**
 * One transfer of {@code count} same-coloured balls from the top of tube {@code from} to the
 * top of tube {@code to}. Immutable.
 */
public final class Move {

    private final int from;
    private final int to;
    private final int count;
    private final int colorIndex;

    public Move(int from, int to, int count, int colorIndex) {
        if (from < 0 || to < 0) {
            throw new IllegalArgumentException("tube indices must be non-negative");
        }
        if (from == to) {
            throw new IllegalArgumentException("a move must change tubes");
        }
        if (count <= 0 || count > Packed.CAPACITY) {
            throw new IllegalArgumentException("illegal ball count: " + count);
        }
        this.from = from;
        this.to = to;
        this.count = count;
        this.colorIndex = colorIndex;
    }

    public int from() {
        return from;
    }

    public int to() {
        return to;
    }

    public int count() {
        return count;
    }

    public int colorIndex() {
        return colorIndex;
    }

    /**
     * @return the move that undoes this one. Only meaningful against the state produced by
     *         applying this move.
     */
    public Move inverse() {
        return new Move(to, from, count, colorIndex);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Move)) {
            return false;
        }
        Move that = (Move) other;
        return from == that.from && to == that.to
            && count == that.count && colorIndex == that.colorIndex;
    }

    @Override
    public int hashCode() {
        int hash = from;
        hash = hash * 31 + to;
        hash = hash * 31 + count;
        hash = hash * 31 + colorIndex;
        return hash;
    }

    @Override
    public String toString() {
        return count + "x c" + colorIndex + " : " + from + " -> " + to;
    }
}
