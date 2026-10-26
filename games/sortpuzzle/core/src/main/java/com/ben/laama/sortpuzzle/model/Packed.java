package com.ben.laama.sortpuzzle.model;

import com.ben.laama.sortpuzzle.GameConfig;

/**
 * Bit-level encoding of a single tube, used everywhere a tube has to be hashed, compared or
 * searched over.
 *
 * <p>A tube is four 4-bit slots packed into the low 16 bits of an {@code int}. Slot 0 (bits
 * 0-3) is the <em>bottom</em> of the tube, slot 3 (bits 12-15) the top. A slot holds
 * {@code colorIndex + 1}, so {@code 0} means "empty" and colour indices {@code 0..9} map to
 * nibbles {@code 1..10}. Balls always occupy a contiguous run from the bottom, so the first
 * zero nibble marks the end of the stack.</p>
 *
 * <p>Everything here is a handful of shifts over at most four slots — no lookup tables, no
 * allocation, safe to call millions of times inside the solver.</p>
 */
public final class Packed {

    /** Balls per tube. */
    public static final int CAPACITY = GameConfig.TUBE_CAPACITY;

    /** Encoding of an empty tube. */
    public static final int EMPTY = 0;

    /** Returned by {@link #top} and {@link #slot} when there is no ball. */
    public static final int NO_COLOR = -1;

    private Packed() {
    }

    /** @return how many balls the encoded tube holds, {@code 0..CAPACITY} */
    public static int size(int encoded) {
        int count = 0;
        while (count < CAPACITY && ((encoded >>> (count << 2)) & 0xF) != 0) {
            count++;
        }
        return count;
    }

    /**
     * @param index 0 = bottom
     * @return the colour index at {@code index}, or {@link #NO_COLOR} when that slot is empty
     */
    public static int slot(int encoded, int index) {
        return ((encoded >>> (index << 2)) & 0xF) - 1;
    }

    /** @return the colour index of the topmost ball, or {@link #NO_COLOR} when empty */
    public static int top(int encoded) {
        int size = size(encoded);
        return size == 0 ? NO_COLOR : slot(encoded, size - 1);
    }

    /** @return how many balls of the top colour sit contiguously at the top, {@code 0} when empty */
    public static int topRun(int encoded) {
        int size = size(encoded);
        if (size == 0) {
            return 0;
        }
        int color = slot(encoded, size - 1);
        int run = 1;
        for (int i = size - 2; i >= 0 && slot(encoded, i) == color; i--) {
            run++;
        }
        return run;
    }

    public static boolean isEmpty(int encoded) {
        return encoded == EMPTY;
    }

    public static boolean isFull(int encoded) {
        return size(encoded) == CAPACITY;
    }

    /** @return free slots left in the tube */
    public static int freeSpace(int encoded) {
        return CAPACITY - size(encoded);
    }

    /** @return {@code true} when the tube is empty or holds a single colour */
    public static boolean isUniform(int encoded) {
        int size = size(encoded);
        if (size <= 1) {
            return true;
        }
        return topRun(encoded) == size;
    }

    /** @return {@code true} when the tube is full and holds a single colour — a finished tube */
    public static boolean isComplete(int encoded) {
        return size(encoded) == CAPACITY && topRun(encoded) == CAPACITY;
    }

    /**
     * Pushes {@code count} balls of {@code color} onto the tube.
     *
     * @throws IllegalArgumentException when the balls would not fit
     */
    public static int push(int encoded, int color, int count) {
        int size = size(encoded);
        if (count < 0 || size + count > CAPACITY) {
            throw new IllegalArgumentException(
                "cannot push " + count + " ball(s) onto a tube of size " + size);
        }
        if (color < 0 || color >= 0xF) {
            throw new IllegalArgumentException("colour index out of range: " + color);
        }
        int result = encoded;
        for (int i = 0; i < count; i++) {
            result |= (color + 1) << ((size + i) << 2);
        }
        return result;
    }

    /**
     * Removes {@code count} balls from the top of the tube.
     *
     * @throws IllegalArgumentException when the tube holds fewer balls than that
     */
    public static int pop(int encoded, int count) {
        int size = size(encoded);
        if (count < 0 || count > size) {
            throw new IllegalArgumentException(
                "cannot pop " + count + " ball(s) from a tube of size " + size);
        }
        int result = encoded;
        for (int i = 1; i <= count; i++) {
            result &= ~(0xF << ((size - i) << 2));
        }
        return result;
    }

    /** Human-readable form, bottom first — {@code "[0,0,3,3]"}. */
    public static String toString(int encoded) {
        StringBuilder out = new StringBuilder("[");
        int size = size(encoded);
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(slot(encoded, i));
        }
        return out.append(']').toString();
    }
}
