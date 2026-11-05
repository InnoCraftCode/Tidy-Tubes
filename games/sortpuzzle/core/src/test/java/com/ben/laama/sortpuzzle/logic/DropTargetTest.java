package com.ben.laama.sortpuzzle.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Geometry of a real two-row board (14 tubes, 7 per row, as laid out by GameScreen on a
 * 720-wide world): tubes 79 wide, 261 tall, 16 apart, rows 70 apart. Row 0 (indices 0-6) is on
 * top, like the screen.
 */
@DisplayName("DropTarget")
class DropTargetTest {

    private static final float W = 79f;
    private static final float H = 261f;
    private static final float GAP = 16f;
    private static final float ROW_GAP = 70f;
    private static final float SIDE = GAP * 0.5f;
    private static final float TOP = 42f + 30f; // lift height + half a ball

    private final float[] x = new float[14];
    private final float[] y = new float[14];
    private final float[] w = new float[14];
    private final float[] h = new float[14];

    @BeforeEach
    void twoRows() {
        for (int i = 0; i < 14; i++) {
            int column = i % 7;
            x[i] = column * (W + GAP);
            y[i] = i < 7 ? H + ROW_GAP : 0f;
            w[i] = W;
            h[i] = H;
        }
    }

    private int pick(float px, float py) {
        return DropTarget.pick(px, py, x, y, w, h, SIDE, TOP);
    }

    private float centreX(int column) {
        return column * (W + GAP) + W * 0.5f;
    }

    @Test
    @DisplayName("balls over a top-row tube pick it, whatever the finger is doing")
    void topRowBody() {
        assertEquals(2, pick(centreX(2), H + ROW_GAP + 100f));
    }

    @Test
    @DisplayName("balls hovering above a bottom-row mouth, in the gap between rows, pick that tube")
    void gapBetweenRowsBelongsToBottomTube() {
        assertEquals(10, pick(centreX(3), H + 40f));
    }

    @Test
    @DisplayName("balls hovering above a top-row mouth pick it")
    void aboveTopRowMouth() {
        assertEquals(5, pick(centreX(5), 2 * H + ROW_GAP + 50f));
    }

    @Test
    @DisplayName("the narrow gap between two neighbours goes to the nearer tube")
    void sideGapGoesToNearest() {
        float rightEdgeOfColumn1 = 1 * (W + GAP) + W;
        assertEquals(8, pick(rightEdgeOfColumn1 + 4f, 100f));
        assertEquals(9, pick(rightEdgeOfColumn1 + GAP - 4f, 100f));
    }

    @Test
    @DisplayName("where the bottom tube's top zone meets the top tube's body, the nearer centre wins")
    void overlapResolvedByDistance() {
        assertEquals(0, pick(centreX(0), H + ROW_GAP + 1f));
    }

    @Test
    @DisplayName("points off the board pick nothing")
    void offBoard() {
        assertEquals(-1, pick(-50f, 100f));
        assertEquals(-1, pick(centreX(0), -10f));
        assertEquals(-1, pick(centreX(0), 2 * H + ROW_GAP + TOP + 1f));
    }

    @Test
    @DisplayName("mismatched arrays are rejected")
    void mismatchedArrays() {
        assertThrows(IllegalArgumentException.class,
            () -> DropTarget.pick(0f, 0f, new float[2], new float[1], new float[2], new float[2], 0f, 0f));
    }
}
