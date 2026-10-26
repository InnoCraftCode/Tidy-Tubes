package com.ben.laama.sortpuzzle.model;

import java.util.ArrayList;
import java.util.List;

/**
 * An immutable tube holding up to {@link Packed#CAPACITY} balls, bottom first.
 *
 * <p>State is kept as a single packed {@code int} (see {@link Packed}), which makes equality,
 * hashing and the solver's transposition table essentially free.</p>
 */
public final class Tube {

    private static final Tube EMPTY = new Tube(Packed.EMPTY);

    private final int encoded;

    private Tube(int encoded) {
        this.encoded = encoded;
    }

    // ------------------------------------------------------------- factories
    public static Tube empty() {
        return EMPTY;
    }

    /**
     * @param colorsBottomFirst colour indices, bottom of the tube first
     */
    public static Tube of(int... colorsBottomFirst) {
        if (colorsBottomFirst.length > Packed.CAPACITY) {
            throw new IllegalArgumentException(
                "a tube holds at most " + Packed.CAPACITY + " balls");
        }
        int encoded = Packed.EMPTY;
        for (int color : colorsBottomFirst) {
            encoded = Packed.push(encoded, color, 1);
        }
        return fromEncoded(encoded);
    }

    /** @param color the colour to fill the tube with */
    public static Tube full(int color) {
        return new Tube(Packed.push(Packed.EMPTY, color, Packed.CAPACITY));
    }

    /** Wraps a raw packed encoding. */
    public static Tube fromEncoded(int encoded) {
        return encoded == Packed.EMPTY ? EMPTY : new Tube(encoded);
    }

    // ---------------------------------------------------------------- queries
    /** @return the raw packed form, for the solver and for hashing */
    public int encoded() {
        return encoded;
    }

    public int size() {
        return Packed.size(encoded);
    }

    public boolean isEmpty() {
        return encoded == Packed.EMPTY;
    }

    public boolean isFull() {
        return Packed.isFull(encoded);
    }

    public int freeSpace() {
        return Packed.freeSpace(encoded);
    }

    /** @return the colour index of the top ball, or {@link Packed#NO_COLOR} when empty */
    public int topColor() {
        return Packed.top(encoded);
    }

    /**
     * @param index 0 = bottom
     * @return the colour index at that position, or {@link Packed#NO_COLOR}
     */
    public int colorAt(int index) {
        if (index < 0 || index >= Packed.CAPACITY) {
            return Packed.NO_COLOR;
        }
        return Packed.slot(encoded, index);
    }

    /** @return how many balls of the top colour sit contiguously at the top */
    public int topRunLength() {
        return Packed.topRun(encoded);
    }

    /** @return {@code true} when empty or single-coloured */
    public boolean isUniform() {
        return Packed.isUniform(encoded);
    }

    /** @return {@code true} when full and single-coloured — a tube the player is done with */
    public boolean isComplete() {
        return Packed.isComplete(encoded);
    }

    /** @return the balls bottom first; allocates, so keep it out of the render loop */
    public List<Ball> balls() {
        int size = size();
        List<Ball> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            out.add(Ball.of(Packed.slot(encoded, i)));
        }
        return out;
    }

    // --------------------------------------------------------------- mutation
    /** @return a new tube with {@code count} balls of {@code color} added on top */
    public Tube push(int color, int count) {
        return new Tube(Packed.push(encoded, color, count));
    }

    /** @return a new tube with {@code count} balls removed from the top */
    public Tube popTop(int count) {
        return fromEncoded(Packed.pop(encoded, count));
    }

    // --------------------------------------------------------------- identity
    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Tube && ((Tube) other).encoded == encoded);
    }

    @Override
    public int hashCode() {
        return encoded;
    }

    @Override
    public String toString() {
        return Packed.toString(encoded);
    }
}
