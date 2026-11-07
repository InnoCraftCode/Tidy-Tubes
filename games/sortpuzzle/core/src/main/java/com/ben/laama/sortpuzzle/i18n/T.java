package com.ben.laama.sortpuzzle.i18n;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Translation lookup. Keys are the English texts themselves, so English needs no file and a
 * missing translation falls back to readable English instead of a key.
 *
 * <ul>
 *   <li>{@link #r} / {@link #f}: text for the game's own renderer. In a right-to-left language
 *       it comes back shaped and in visual order (see {@link ArabicText}), ready for a label.</li>
 *   <li>{@link #raw} / {@link #rawf}: logical text for anything the platform renders itself
 *       (notifications, share sheet), and for arguments of {@link #f}, which shapes the result
 *       as a whole. Never pass the output of {@code r}/{@code f} into {@code f}.</li>
 * </ul>
 *
 * <p>Placeholders are {@code {0}}, {@code {1}}...; apostrophes are plain text (no
 * {@code MessageFormat} quoting). The table is replaced as a whole on a language change, on the
 * render thread; reads never see a half-built map.</p>
 */
public final class T {

    private static volatile Language language = Language.EN;
    private static volatile Map<String, String> table = Collections.emptyMap();

    private T() {
    }

    /** Switches language. {@code translations} maps English to the language; may be empty. */
    public static void install(Language lang, Map<String, String> translations) {
        Map<String, String> copy = translations == null
            ? Collections.<String, String>emptyMap()
            : Collections.unmodifiableMap(new HashMap<>(translations));
        table = copy;
        language = lang == null ? Language.EN : lang;
    }

    public static Language language() {
        return language;
    }

    public static boolean rtl() {
        return language.rtl;
    }

    /**
     * Marks an English literal as a translation key without translating it (arrays, enum
     * constants, catalogue words that are translated later where they are shown). Returns its
     * argument; exists so the key scan in {@code TranslationFilesTest} finds every key.
     */
    public static String k(String english) {
        return english;
    }

    /** Translated text, ready to draw. */
    public static String r(String english) {
        return show(lookup(english));
    }

    /** Translated pattern with its {@code {n}} placeholders filled, ready to draw. */
    public static String f(String english, Object... args) {
        return show(format(lookup(english), args));
    }

    /** Translated text in logical order, for the platform or as an argument of {@link #f}. */
    public static String raw(String english) {
        return lookup(english);
    }

    public static String rawf(String english, Object... args) {
        return format(lookup(english), args);
    }

    /** Prepares any logical text for the game's renderer (no-op left to right). */
    public static String show(String logical) {
        return language.rtl ? ArabicText.visual(logical) : logical;
    }

    /** @return {@code true} when the current language has its own text for this key */
    public static boolean has(String english) {
        String value = table.get(english);
        return value != null && !value.isEmpty();
    }

    static String lookup(String english) {
        if (english == null) {
            return "";
        }
        String value = table.get(english);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        // "Deep Sea II", "Ruby Harbor 12": translate the name, keep the numbering.
        int space = english.lastIndexOf(' ');
        if (space > 0 && isNumbering(english.substring(space + 1))) {
            String base = table.get(english.substring(0, space));
            if (base != null && !base.isEmpty()) {
                return base + english.substring(space);
            }
        }
        return english;
    }

    private static boolean isNumbering(String word) {
        if (word.isEmpty()) return false;
        boolean roman = true;
        boolean digits = true;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            roman &= c == 'I' || c == 'V' || c == 'X';
            digits &= Character.isDigit(c);
        }
        return roman || digits;
    }

    static String format(String pattern, Object... args) {
        if (args == null || args.length == 0 || pattern.indexOf('{') < 0) {
            return pattern;
        }
        StringBuilder out = new StringBuilder(pattern.length() + 16);
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '{') {
                int close = pattern.indexOf('}', i);
                if (close > i + 1) {
                    String index = pattern.substring(i + 1, close);
                    if (isDigits(index)) {
                        int n = Integer.parseInt(index);
                        out.append(n < args.length ? String.valueOf(args[n]) : "");
                        i = close + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static boolean isDigits(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return !text.isEmpty() && text.length() < 3;
    }
}
