package com.ben.laama.sortpuzzle.i18n;

import java.text.Bidi;

/**
 * Makes Arabic drawable by a renderer that only lays glyphs out left to right, one code point
 * per glyph (libGDX's {@code BitmapFont}): letters are swapped for their contextual
 * presentation forms (isolated, final, initial, medial, plus the lam-alef ligatures), then the
 * line is put in visual order with the Unicode bidirectional algorithm ({@link Bidi}), so
 * numbers and Latin words inside an Arabic sentence keep reading left to right.
 *
 * <p>Plain Java, no libGDX: unit-tested on the desktop JVM, and {@code java.text.Bidi} is part
 * of Android. Diacritics (harakat) are not used by the game's texts and are passed through as
 * transparent marks.</p>
 */
public final class ArabicText {

    private ArabicText() {
    }

    /** Joining behaviour of a base letter. */
    private static final int NONE = 0;   // does not join (hamza, Latin, space...)
    private static final int RIGHT = 1;  // joins to the letter before only (alef, dal, ra, waw...)
    private static final int DUAL = 2;   // joins on both sides

    /**
     * Presentation forms for U+0621..U+064A: {isolated, final, initial, medial}; 0 where the
     * form does not exist (right-joining letters have no initial or medial form).
     */
    private static final int[][] FORMS = new int[0x064A - 0x0621 + 1][];

    static {
        put(0x0621, 0xFE80, 0, 0, 0);
        right(0x0622, 0xFE81);
        right(0x0623, 0xFE83);
        right(0x0624, 0xFE85);
        right(0x0625, 0xFE87);
        dual(0x0626, 0xFE89);
        right(0x0627, 0xFE8D);
        dual(0x0628, 0xFE8F);
        right(0x0629, 0xFE93);
        dual(0x062A, 0xFE95);
        dual(0x062B, 0xFE99);
        dual(0x062C, 0xFE9D);
        dual(0x062D, 0xFEA1);
        dual(0x062E, 0xFEA5);
        right(0x062F, 0xFEA9);
        right(0x0630, 0xFEAB);
        right(0x0631, 0xFEAD);
        right(0x0632, 0xFEAF);
        dual(0x0633, 0xFEB1);
        dual(0x0634, 0xFEB5);
        dual(0x0635, 0xFEB9);
        dual(0x0636, 0xFEBD);
        dual(0x0637, 0xFEC1);
        dual(0x0638, 0xFEC5);
        dual(0x0639, 0xFEC9);
        dual(0x063A, 0xFECD);
        dual(0x0641, 0xFED1);
        dual(0x0642, 0xFED5);
        dual(0x0643, 0xFED9);
        dual(0x0644, 0xFEDD);
        dual(0x0645, 0xFEE1);
        dual(0x0646, 0xFEE5);
        dual(0x0647, 0xFEE9);
        right(0x0648, 0xFEED);
        right(0x0649, 0xFEEF);
        dual(0x064A, 0xFEF1);
    }

    private static void put(int letter, int isolated, int fin, int initial, int medial) {
        FORMS[letter - 0x0621] = new int[] {isolated, fin, initial, medial};
    }

    private static void right(int letter, int first) {
        put(letter, first, first + 1, 0, 0);
    }

    private static void dual(int letter, int first) {
        put(letter, first, first + 1, first + 2, first + 3);
    }

    private static final char TATWEEL = 'ـ';
    private static final char LAM = 'ل';

    private static int joining(char c) {
        if (c == TATWEEL) {
            return DUAL;
        }
        if (c < 0x0621 || c > 0x064A) {
            return NONE;
        }
        int[] forms = FORMS[c - 0x0621];
        if (forms == null) {
            return NONE;
        }
        return forms[2] != 0 ? DUAL : forms[1] != 0 ? RIGHT : NONE;
    }

    /** Harakat and other marks that do not break the joining between their neighbours. */
    private static boolean transparent(char c) {
        return (c >= 0x064B && c <= 0x065F) || c == 0x0670;
    }

    /** Lam + this alef makes a ligature: returns its isolated form, or 0. */
    private static int lamAlef(char alef) {
        switch (alef) {
            case 'آ': return 0xFEF5;
            case 'أ': return 0xFEF7;
            case 'إ': return 0xFEF9;
            case 'ا': return 0xFEFB;
            default: return 0;
        }
    }

    /** @return {@code true} when the text holds at least one Arabic letter */
    public static boolean hasArabic(CharSequence text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 0x0600 && c <= 0x06FF) || (c >= 0xFE70 && c <= 0xFEFF)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Contextual shaping, still in logical order.
     *
     * @param text logical Arabic (or mixed) text
     * @return the same text with letters replaced by their presentation forms
     */
    public static String shape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (transparent(c)) {
                // Harakat (shadda, tanween...): a BitmapFont cannot stack a mark on its letter,
                // it would advance like a letter. UI Arabic reads fine without them.
                continue;
            }
            int type = joining(c);
            if (type == NONE || c == TATWEEL) {
                boolean letter = c >= 0x0621 && c <= 0x064A && FORMS[c - 0x0621] != null;
                out.append(letter ? (char) FORMS[c - 0x0621][0] : c); // hamza: isolated form
                continue;
            }
            boolean joinsBefore = previousJoins(text, i);
            // Lam-alef: one ligature glyph for the pair.
            if (c == LAM) {
                int next = nextBase(text, i);
                if (next > 0) {
                    int ligature = lamAlef(text.charAt(next));
                    if (ligature != 0) {
                        out.append((char) (joinsBefore ? ligature + 1 : ligature));
                        i = next; // marks between lam and alef are dropped with the alef
                        continue;
                    }
                }
            }
            boolean joinsAfter = type == DUAL && nextJoins(text, i);
            int[] forms = FORMS[c - 0x0621];
            int form;
            if (joinsBefore && joinsAfter) {
                form = forms[3];
            } else if (joinsBefore) {
                form = forms[1];
            } else if (joinsAfter) {
                form = forms[2];
            } else {
                form = forms[0];
            }
            out.append((char) (form == 0 ? forms[0] : form));
        }
        return out.toString();
    }

    /** The letter before {@code i} (skipping marks) joins forward, so {@code i} joins back. */
    private static boolean previousJoins(String text, int i) {
        for (int k = i - 1; k >= 0; k--) {
            char p = text.charAt(k);
            if (transparent(p)) continue;
            return joining(p) == DUAL;
        }
        return false;
    }

    private static boolean nextJoins(String text, int i) {
        int next = nextBase(text, i);
        return next > 0 && joining(text.charAt(next)) != NONE;
    }

    private static int nextBase(String text, int i) {
        for (int k = i + 1; k < text.length(); k++) {
            if (!transparent(text.charAt(k))) return k;
        }
        return -1;
    }

    /**
     * Shapes the text and puts every line in visual (left-to-right drawing) order, with a
     * right-to-left paragraph direction. Lines split on {@code '\n'} are handled one by one and
     * stay in order. Text without Arabic is returned as it is.
     */
    public static String visual(String logical) {
        if (logical == null || logical.isEmpty() || !hasArabic(logical)) {
            return logical;
        }
        if (logical.indexOf('\n') >= 0) {
            String[] lines = logical.split("\n", -1);
            StringBuilder out = new StringBuilder(logical.length());
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) out.append('\n');
                out.append(visualLine(lines[i]));
            }
            return out.toString();
        }
        return visualLine(logical);
    }

    private static String visualLine(String logical) {
        if (logical.isEmpty()) {
            return logical;
        }
        String shaped = shape(logical);
        Bidi bidi = new Bidi(shaped, Bidi.DIRECTION_RIGHT_TO_LEFT);
        int runs = bidi.getRunCount();
        byte[] levels = new byte[runs];
        Integer[] order = new Integer[runs];
        for (int r = 0; r < runs; r++) {
            levels[r] = (byte) bidi.getRunLevel(r);
            order[r] = r;
        }
        Bidi.reorderVisually(levels, 0, order, 0, runs);
        StringBuilder out = new StringBuilder(shaped.length());
        for (int v = 0; v < runs; v++) {
            int r = order[v];
            int start = bidi.getRunStart(r);
            int limit = bidi.getRunLimit(r);
            if ((bidi.getRunLevel(r) & 1) == 1) {
                for (int k = limit - 1; k >= start; k--) {
                    out.append(mirror(shaped.charAt(k)));
                }
            } else {
                out.append(shaped, start, limit);
            }
        }
        return out.toString();
    }

    /** Paired punctuation flips in a right-to-left run. */
    private static char mirror(char c) {
        switch (c) {
            case '(': return ')';
            case ')': return '(';
            case '[': return ']';
            case ']': return '[';
            case '{': return '}';
            case '}': return '{';
            case '<': return '>';
            case '>': return '<';
            case '«': return '»';
            case '»': return '«';
            case '→': return '←';
            case '←': return '→';
            default: return c;
        }
    }
}
