package com.ben.laama.sortpuzzle.model;

import java.util.Arrays;

/**
 * An immutable snapshot of the whole board. Applying a {@link Move} returns a new instance,
 * which is what makes unlimited undo a one-liner and the solver's search trivially safe.
 *
 * <p>Internally it is nothing but an {@code int} per tube (see {@link Packed}), so a state is
 * cheap to copy, compare and hash.</p>
 */
public final class BoardState {

    private final int[] encoded;
    private final int colorCount;
    private int hash;

    private BoardState(int[] encoded, int colorCount) {
        this.encoded = encoded;
        this.colorCount = colorCount;
    }

    // ------------------------------------------------------------- factories
    public static BoardState of(Tube... tubes) {
        int[] encoded = new int[tubes.length];
        for (int i = 0; i < tubes.length; i++) {
            encoded[i] = tubes[i].encoded();
        }
        return fromEncoded(encoded);
    }

    /** Takes ownership of {@code encoded}; callers must not keep a reference to the array. */
    public static BoardState fromEncoded(int[] encoded) {
        return new BoardState(encoded, countColors(encoded));
    }

    /** Defensive variant for callers that want to keep their array. */
    public static BoardState copyOfEncoded(int[] encoded) {
        return fromEncoded(Arrays.copyOf(encoded, encoded.length));
    }

    /**
     * The goal configuration: one full tube per colour, followed by {@code emptyTubes} empties.
     *
     * @param colors     number of distinct colours, each contributing exactly one full tube
     * @param emptyTubes spare tubes the player manoeuvres with
     */
    public static BoardState solved(int colors, int emptyTubes) {
        if (colors <= 0) {
            throw new IllegalArgumentException("a board needs at least one colour");
        }
        if (emptyTubes < 0) {
            throw new IllegalArgumentException("empty tube count cannot be negative");
        }
        int[] encoded = new int[colors + emptyTubes];
        for (int i = 0; i < colors; i++) {
            encoded[i] = Packed.push(Packed.EMPTY, i, Packed.CAPACITY);
        }
        return new BoardState(encoded, colors);
    }

    private static int countColors(int[] encoded) {
        int highest = -1;
        for (int tube : encoded) {
            int size = Packed.size(tube);
            for (int slot = 0; slot < size; slot++) {
                highest = Math.max(highest, Packed.slot(tube, slot));
            }
        }
        return highest + 1;
    }

    // ---------------------------------------------------------------- queries
    public int tubeCount() {
        return encoded.length;
    }

    /** @return the number of distinct colours present, i.e. highest colour index + 1 */
    public int colorCount() {
        return colorCount;
    }

    public Tube tube(int index) {
        return Tube.fromEncoded(encoded[index]);
    }

    /** @return the packed encoding of one tube — the allocation-free accessor */
    public int encodedAt(int index) {
        return encoded[index];
    }

    /** @return a copy of the packed encodings, safe for the caller to mutate */
    public int[] toEncodedArray() {
        return Arrays.copyOf(encoded, encoded.length);
    }

    public int emptyTubeCount() {
        int count = 0;
        for (int tube : encoded) {
            if (tube == Packed.EMPTY) {
                count++;
            }
        }
        return count;
    }

    public int completeTubeCount() {
        int count = 0;
        for (int tube : encoded) {
            if (Packed.isComplete(tube)) {
                count++;
            }
        }
        return count;
    }

    /** @return {@code true} when every non-empty tube is full and single-coloured */
    public boolean isSolved() {
        for (int tube : encoded) {
            if (tube != Packed.EMPTY && !Packed.isComplete(tube)) {
                return false;
            }
        }
        return true;
    }

    // --------------------------------------------------------------- mutation
    /**
     * Applies a move mechanically — the balls are moved, the rules are not re-checked here.
     * Use {@link com.ben.laama.sortpuzzle.logic.MoveValidator} to decide whether a move is legal.
     *
     * @throws IllegalArgumentException when the transfer is physically impossible
     */
    public BoardState apply(Move move) {
        int from = move.from();
        int to = move.to();
        if (from >= encoded.length || to >= encoded.length) {
            throw new IllegalArgumentException("tube index out of range in " + move);
        }
        int[] next = Arrays.copyOf(encoded, encoded.length);
        next[from] = Packed.pop(next[from], move.count());
        next[to] = Packed.push(next[to], move.colorIndex(), move.count());
        return new BoardState(next, colorCount);
    }

    /** @return the same board with one more empty tube — the "extra tube" booster */
    public BoardState withExtraTube() {
        int[] next = Arrays.copyOf(encoded, encoded.length + 1);
        next[encoded.length] = Packed.EMPTY;
        return new BoardState(next, colorCount);
    }

    // ---------------------------------------------------------------- hashing
    /**
     * Order-independent fingerprint: tubes are interchangeable, so two boards that differ only
     * by a permutation of their tubes collapse onto the same key. This is what keeps the
     * solver's transposition table from exploring the same position over and over.
     *
     * @return four packed tubes per {@code long}, ascending
     */
    public long[] canonicalKey() {
        int[] sorted = Arrays.copyOf(encoded, encoded.length);
        Arrays.sort(sorted);
        long[] key = new long[(sorted.length + 3) >>> 2];
        for (int i = 0; i < sorted.length; i++) {
            key[i >>> 2] |= ((long) (sorted[i] & 0xFFFF)) << ((i & 3) << 4);
        }
        return key;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardState)) {
            return false;
        }
        return Arrays.equals(encoded, ((BoardState) other).encoded);
    }

    @Override
    public int hashCode() {
        int cached = hash;
        if (cached == 0) {
            cached = Arrays.hashCode(encoded);
            if (cached == 0) {
                cached = 1; // 0 is the "not computed yet" sentinel
            }
            hash = cached;
        }
        return cached;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < encoded.length; i++) {
            if (i > 0) {
                out.append(' ');
            }
            out.append(Packed.toString(encoded[i]));
        }
        return out.toString();
    }
}
