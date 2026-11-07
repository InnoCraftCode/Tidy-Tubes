package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Mirrors scene2d layouts for a right-to-left language, after the fact: every {@link Table}
 * row is reversed (cell contents and their sizing swap ends), left and right paddings, spacings
 * and alignments are exchanged, labels align to the other side, and wrapped labels holding
 * visual-order Arabic are line-broken from the right end of the sentence.
 *
 * <p>Lives in libGDX's package because it swaps {@link Cell} fields that have no public setter
 * for "whatever was there". Screens are built left to right as usual; {@code BaseScreen} calls
 * {@link #apply} each frame while an RTL language is active. Each row, table and label records
 * the state it was left in, so a mirrored row is never flipped back, while a row rebuilt by
 * {@code clearChildren()} (rotation, a refreshed list) is mirrored again.</p>
 */
@SuppressWarnings("rawtypes")
public final class RtlMirror {

    private static final WeakHashMap<Table, int[]> ROWS = new WeakHashMap<>();
    private static final WeakHashMap<Table, Integer> TABLES = new WeakHashMap<>();
    private static final WeakHashMap<Label, Integer> LABELS = new WeakHashMap<>();
    private static final WeakHashMap<Label, Object[]> WRAPS = new WeakHashMap<>();
    private static final GlyphLayout MEASURE = new GlyphLayout();

    private RtlMirror() {
    }

    /** Mirrors everything under {@code root} that is not mirrored yet. */
    public static void apply(Actor root) {
        walk(root);
    }

    private static void walk(Actor actor) {
        if (actor instanceof Table) {
            mirrorTable((Table) actor);
        } else if (actor instanceof Label) {
            mirrorLabel((Label) actor);
        }
        if (actor instanceof Group) {
            Array<Actor> children = ((Group) actor).getChildren();
            for (int i = 0; i < children.size; i++) {
                walk(children.get(i));
            }
        }
    }

    // ---------------------------------------------------------------- tables
    private static void mirrorTable(Table table) {
        int tableState = System.identityHashCode(table.getPadLeftValue()) * 31
            + System.identityHashCode(table.getPadRightValue()) * 7 + table.getAlign();
        Integer known = TABLES.get(table);
        if (known == null || known != tableState) {
            Value left = table.getPadLeftValue();
            table.padLeft(table.getPadRightValue());
            table.padRight(left);
            table.align(flip(table.getAlign()));
            TABLES.put(table, System.identityHashCode(table.getPadLeftValue()) * 31
                + System.identityHashCode(table.getPadRightValue()) * 7 + table.getAlign());
        }

        Array<Cell> cells = table.getCells();
        if (cells.size == 0) {
            return;
        }
        int rows = cells.peek().row + 1;
        int[] states = ROWS.get(table);
        if (states == null || states.length < rows) {
            int[] grown = new int[Math.max(rows, 4)];
            if (states != null) System.arraycopy(states, 0, grown, 0, states.length);
            states = grown;
            ROWS.put(table, states);
        }
        boolean changed = false;
        int start = 0;
        while (start < cells.size) {
            int row = cells.get(start).row;
            int end = start;
            while (end < cells.size && cells.get(end).row == row) end++;
            int state = signature(cells, start, end);
            if (states[row] != state || state == 0) {
                mirrorRow(cells, start, end);
                states[row] = signature(cells, start, end);
                if (states[row] == 0) states[row] = 1;
                changed = true;
            }
            start = end;
        }
        if (changed) {
            table.invalidate();
        }
    }

    private static int signature(Array<Cell> cells, int start, int end) {
        int hash = 17;
        for (int i = start; i < end; i++) {
            Cell cell = cells.get(i);
            hash = hash * 31 + System.identityHashCode(cell);
            hash = hash * 31 + System.identityHashCode(cell.actor);
            hash = hash * 31 + (cell.align == null ? 0 : cell.align);
            hash = hash * 31 + System.identityHashCode(cell.padLeft);
            hash = hash * 31 + System.identityHashCode(cell.padRight);
        }
        return hash;
    }

    private static void mirrorRow(Array<Cell> cells, int start, int end) {
        boolean spans = false;
        for (int i = start; i < end; i++) {
            Integer span = cells.get(i).colspan;
            if (span != null && span != 1) spans = true;
        }
        if (!spans) {
            for (int i = start, j = end - 1; i < j; i++, j--) {
                swap(cells.get(i), cells.get(j));
            }
        }
        for (int i = start; i < end; i++) {
            Cell cell = cells.get(i);
            Value pad = cell.padLeft;
            cell.padLeft = cell.padRight;
            cell.padRight = pad;
            Value space = cell.spaceLeft;
            cell.spaceLeft = cell.spaceRight;
            cell.spaceRight = space;
            if (cell.align != null) cell.align = flip(cell.align);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void swap(Cell a, Cell b) {
        Actor actor = a.actor; a.actor = b.actor; b.actor = actor;
        Value v;
        v = a.minWidth; a.minWidth = b.minWidth; b.minWidth = v;
        v = a.minHeight; a.minHeight = b.minHeight; b.minHeight = v;
        v = a.prefWidth; a.prefWidth = b.prefWidth; b.prefWidth = v;
        v = a.prefHeight; a.prefHeight = b.prefHeight; b.prefHeight = v;
        v = a.maxWidth; a.maxWidth = b.maxWidth; b.maxWidth = v;
        v = a.maxHeight; a.maxHeight = b.maxHeight; b.maxHeight = v;
        v = a.spaceTop; a.spaceTop = b.spaceTop; b.spaceTop = v;
        v = a.spaceBottom; a.spaceBottom = b.spaceBottom; b.spaceBottom = v;
        v = a.spaceLeft; a.spaceLeft = b.spaceLeft; b.spaceLeft = v;
        v = a.spaceRight; a.spaceRight = b.spaceRight; b.spaceRight = v;
        v = a.padTop; a.padTop = b.padTop; b.padTop = v;
        v = a.padBottom; a.padBottom = b.padBottom; b.padBottom = v;
        v = a.padLeft; a.padLeft = b.padLeft; b.padLeft = v;
        v = a.padRight; a.padRight = b.padRight; b.padRight = v;
        Float f;
        f = a.fillX; a.fillX = b.fillX; b.fillX = f;
        f = a.fillY; a.fillY = b.fillY; b.fillY = f;
        Integer i;
        i = a.align; a.align = b.align; b.align = i;
        i = a.expandX; a.expandX = b.expandX; b.expandX = i;
        i = a.expandY; a.expandY = b.expandY; b.expandY = i;
        Boolean u;
        u = a.uniformX; a.uniformX = b.uniformX; b.uniformX = u;
        u = a.uniformY; a.uniformY = b.uniformY; b.uniformY = u;
    }

    private static int flip(int align) {
        boolean left = (align & Align.left) != 0;
        boolean right = (align & Align.right) != 0;
        if (left == right) return align;
        align &= ~(Align.left | Align.right);
        return align | (left ? Align.right : Align.left);
    }

    // ---------------------------------------------------------------- labels
    private static void mirrorLabel(Label label) {
        Integer known = LABELS.get(label);
        int current = label.getLabelAlign() * 64 + label.getLineAlign();
        if (known == null || known != current) {
            label.setAlignment(flip(label.getLabelAlign()), flip(label.getLineAlign()));
            LABELS.put(label, label.getLabelAlign() * 64 + label.getLineAlign());
        }
        if (label.getWrap()) {
            wrap(label);
        }
    }

    /**
     * BitmapFont wraps from the left end of a line; a visual-order Arabic line read that way
     * puts the end of the sentence on the first line. Break it ourselves, from the logical start
     * (the right end), and hand the label lines that already fit.
     */
    private static void wrap(Label label) {
        float width = label.getWidth();
        String text = label.getText().toString();
        Object[] state = WRAPS.get(label);
        String source = text;
        if (state != null && text.equals(state[1])) {
            if ((Float) state[2] == width) return;
            source = (String) state[0];
        }
        if (width <= 0f || !hasArabic(source)) {
            return;
        }
        BitmapFont font = label.getStyle().font;
        float scaleX = font.getData().scaleX;
        float scaleY = font.getData().scaleY;
        // Same scale the label measures with (Label.setFontScale is absolute, see Ui.setTextScale).
        font.getData().setScale(com.ben.laama.sortpuzzle.ui.Ui.effectiveFontScaleX(label),
            com.ben.laama.sortpuzzle.ui.Ui.effectiveFontScaleY(label));
        StringBuilder out = new StringBuilder(source.length() + 8);
        try {
            String[] paragraphs = source.split("\n", -1);
            for (int p = 0; p < paragraphs.length; p++) {
                if (p > 0) out.append('\n');
                breakLine(font, paragraphs[p], width, out);
            }
        } finally {
            font.getData().setScale(scaleX, scaleY);
        }
        String produced = out.toString();
        WRAPS.put(label, new Object[] {source, produced, width});
        if (!produced.equals(text)) {
            label.setText(produced);
        }
    }

    private static void breakLine(BitmapFont font, String visual, float width, StringBuilder out) {
        String[] words = visual.split(" ");
        List<List<String>> lines = new ArrayList<>();
        List<String> line = new ArrayList<>();
        StringBuilder probe = new StringBuilder();
        // visual order is reversed: the logical first word is the last one
        for (int i = words.length - 1; i >= 0; i--) {
            String word = words[i];
            probe.setLength(0);
            probe.append(word);
            for (String w : line) probe.append(' ').append(w);
            MEASURE.setText(font, probe);
            if (!line.isEmpty() && MEASURE.width > width) {
                lines.add(line);
                line = new ArrayList<>();
            }
            line.add(0, word);
        }
        if (!line.isEmpty()) lines.add(line);
        for (int l = 0; l < lines.size(); l++) {
            if (l > 0) out.append('\n');
            List<String> words2 = lines.get(l);
            for (int w = 0; w < words2.size(); w++) {
                if (w > 0) out.append(' ');
                out.append(words2.get(w));
            }
        }
    }

    private static boolean hasArabic(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 0x0600 && c <= 0x06FF) || (c >= 0xFE70 && c <= 0xFEFF)) return true;
        }
        return false;
    }
}
