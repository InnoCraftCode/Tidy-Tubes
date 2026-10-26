package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.model.BoardState;

/** A generated level: its starting board plus the parameters that produced it. */
public final class Level {

    /** {@link #parMoves} when the optimal count has not been computed. */
    public static final int PAR_UNKNOWN = 0;

    private final int index;
    private final BoardState start;
    private final int colors;
    private final int emptyTubes;
    private final int shuffleDepth;
    private final int parMoves;

    Level(int index, BoardState start, int colors, int emptyTubes, int shuffleDepth,
          int parMoves) {
        this.index = index;
        this.start = start;
        this.colors = colors;
        this.emptyTubes = emptyTubes;
        this.shuffleDepth = shuffleDepth;
        this.parMoves = parMoves;
    }

    /** 1-based level number. */
    public int index() {
        return index;
    }

    public BoardState start() {
        return start;
    }

    public int colors() {
        return colors;
    }

    public int emptyTubes() {
        return emptyTubes;
    }

    public int tubeCount() {
        return start.tubeCount();
    }

    public int shuffleDepth() {
        return shuffleDepth;
    }

    /** @return the optimal move count, or {@link #PAR_UNKNOWN} when it was not computed */
    public int parMoves() {
        return parMoves;
    }

    /** @return a copy of this level carrying a freshly computed par */
    public Level withPar(int newPar) {
        return new Level(index, start, colors, emptyTubes, shuffleDepth, newPar);
    }

    @Override
    public String toString() {
        return "Level " + index + " (" + colors + " colours, " + emptyTubes
            + " empty, par " + (parMoves == PAR_UNKNOWN ? "?" : parMoves) + ")";
    }
}
