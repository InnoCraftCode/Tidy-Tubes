package com.ben.laama.sortpuzzle.i18n;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates the catalogue's generated names and descriptions, which are built from parts
 * ("Ruby Harbor II", "Glossy shine with polka dots") and so cannot all be listed as keys.
 *
 * <p>A two-word name is a modifier and a noun; each is a key, and the key
 * {@value #ORDER} tells the language how to combine them ({@code "{1} {0}"} where the noun comes
 * first: French "Port rubis", Arabic, Indonesian...). Hand-made names are plain keys and win
 * over the composition. Everything returned here is logical text ({@link T#raw}): pass it to
 * {@link T#f} or {@link T#show}.</p>
 */
public final class Names {

    /** Word order of a generated two-word name: modifier {0}, noun {1}. */
    public static final String ORDER = "{0} {1} (style name)";

    private static final Pattern FINISH = Pattern.compile("^(\\S+) shine with (.+?)(, bolder)?$");
    private static final Pattern THEME = Pattern.compile("^(\\S+) (\\S+), (\\S+) light$");

    private Names() {
    }

    /** @return the cosmetic or chapter name in the interface language, logical order */
    public static String name(String english) {
        if (english == null || english.isEmpty() || T.has(english)) {
            return T.raw(english);
        }
        String base = english;
        String numbering = "";
        int space = english.lastIndexOf(' ');
        if (space > 0 && isNumbering(english.substring(space + 1))) {
            base = english.substring(0, space);
            numbering = english.substring(space);
            if (T.has(base)) {
                return T.raw(base) + numbering;
            }
        }
        int split = base.indexOf(' ');
        if (split <= 0) {
            return T.raw(base) + numbering;
        }
        String modifier = base.substring(0, split);
        String noun = base.substring(split + 1);
        // The key carries a note for translators; English (no table) must not show it.
        String order = T.has(ORDER) ? ORDER : "{0} {1}";
        return T.rawf(order, T.raw(modifier), T.raw(noun)) + numbering;
    }

    /** @return the description in the interface language, logical order */
    public static String description(String english) {
        if (english == null || english.isEmpty() || T.has(english)) {
            return T.raw(english);
        }
        Matcher finish = FINISH.matcher(english);
        if (finish.matches()) {
            String text = T.rawf("{0} shine with {1}", T.raw(finish.group(1)), T.raw(finish.group(2)));
            return finish.group(3) == null ? text : text + T.raw(", bolder");
        }
        Matcher theme = THEME.matcher(english);
        if (theme.matches()) {
            return T.rawf("{0} {1}, {2} light", T.raw(theme.group(1)),
                inSentence(theme.group(2)), inSentence(theme.group(3)));
        }
        return T.raw(english);
    }

    /**
     * A catalogue word used mid-sentence ("harbor" in "Dusk harbor, ruby light"): the key is the
     * capitalised word, the translation is lower-cased again, except in German, which
     * capitalises nouns.
     */
    private static String inSentence(String lowerWord) {
        String key = Character.toUpperCase(lowerWord.charAt(0)) + lowerWord.substring(1);
        if (!T.has(key)) {
            return lowerWord;
        }
        String word = T.raw(key);
        if (T.language() == Language.DE || word.isEmpty()) {
            return word;
        }
        java.util.Locale locale = new java.util.Locale(T.language().code);
        return word.substring(0, 1).toLowerCase(locale) + word.substring(1);
    }

    private static boolean isNumbering(String word) {
        if (word.isEmpty()) return false;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (c != 'I' && c != 'V' && c != 'X' && !Character.isDigit(c)) return false;
        }
        return true;
    }
}
