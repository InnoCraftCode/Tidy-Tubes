package com.ben.laama.sortpuzzle.model;

import com.ben.laama.sortpuzzle.GameConfig;

/**
 * A single ball, identified purely by its colour index.
 *
 * <p>Instances are interned, so {@code Ball.of(3) == Ball.of(3)} and the game never allocates
 * a ball at runtime. Boards themselves store colours as packed nibbles (see {@link Packed});
 * this type exists for readable APIs and tests.</p>
 */
public final class Ball {

    private static final Ball[] CACHE = new Ball[GameConfig.MAX_COLORS];

    static {
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new Ball(i);
        }
    }

    private final int colorIndex;

    private Ball(int colorIndex) {
        this.colorIndex = colorIndex;
    }

    /**
     * @param colorIndex {@code 0 .. GameConfig.MAX_COLORS - 1}
     * @return the interned ball for that colour
     */
    public static Ball of(int colorIndex) {
        if (colorIndex < 0 || colorIndex >= CACHE.length) {
            throw new IllegalArgumentException("colour index out of range: " + colorIndex);
        }
        return CACHE[colorIndex];
    }

    public int colorIndex() {
        return colorIndex;
    }

    @Override
    public boolean equals(Object other) {
        // Interned, so identity is enough — but stay well behaved anyway.
        return this == other || (other instanceof Ball && ((Ball) other).colorIndex == colorIndex);
    }

    @Override
    public int hashCode() {
        return colorIndex;
    }

    @Override
    public String toString() {
        return "Ball(" + colorIndex + ")";
    }
}
