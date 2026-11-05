package com.ben.laama.sortpuzzle.ui;

/**
 * Picks the number of rows and the tube size that make the balls as large as possible inside
 * a board area of any shape (portrait column, landscape band, tablet). Pure arithmetic with a
 * unit test; {@code GameScreen} turns the result into tube positions.
 */
public final class BoardLayout {

    public final int rows;
    public final int perRow;
    public final float tubeWidth;
    public final float ballSize;
    public final float tubeHeight;

    private BoardLayout(int rows, int perRow, float tubeWidth, float padding, int capacity) {
        this.rows = rows;
        this.perRow = perRow;
        this.tubeWidth = tubeWidth;
        this.ballSize = tubeWidth - 2f * padding;
        this.tubeHeight = ballSize * capacity + 2f * padding;
    }

    /** Height of all rows plus the gaps between them. */
    public float stackHeight(float rowGap) {
        return rows * tubeHeight + (rows - 1) * rowGap;
    }

    /**
     * @param count       tubes on the board
     * @param areaWidth   world units available across
     * @param areaHeight  world units available up the screen
     * @param maxWidth    largest tube width allowed (the design size)
     * @param gap         horizontal gap between tubes
     * @param rowGap      vertical gap between rows
     * @param maxPerRow   most tubes a row may hold before another row is preferred
     * @param padding     space between a ball and the tube wall
     * @param capacity    balls per tube
     */
    public static BoardLayout fit(int count, float areaWidth, float areaHeight, float maxWidth,
                                  float gap, float rowGap, int maxPerRow, float padding,
                                  int capacity) {
        int tubes = Math.max(1, count);
        BoardLayout best = null;
        for (int rows = 1; rows <= 3; rows++) {
            int perRow = (tubes + rows - 1) / rows;
            if (perRow > maxPerRow && rows < 3) {
                continue;
            }
            float width = Math.min(maxWidth, (areaWidth - gap * (perRow - 1)) / perRow);
            float heightLimitedBall = ((areaHeight - (rows - 1) * rowGap) / rows - 2f * padding)
                / capacity;
            width = Math.min(width, heightLimitedBall + 2f * padding);
            width = Math.max(width, 2f * padding + 8f); // never collapse to nothing
            if (best == null || width > best.tubeWidth + 0.5f) {
                best = new BoardLayout(rows, perRow, width, padding, capacity);
            }
        }
        return best;
    }
}
