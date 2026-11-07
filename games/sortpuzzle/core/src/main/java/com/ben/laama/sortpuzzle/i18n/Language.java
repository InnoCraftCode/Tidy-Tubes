package com.ben.laama.sortpuzzle.i18n;

import java.util.Locale;

/**
 * The languages the game is translated into, in the order the picker lists them.
 *
 * <p>Each one carries the country whose flag stands for it in the picker (the store listing's
 * locale: es-ES, pt-BR, ar for Saudi Arabia...), its own name written in itself, and whether it
 * reads right to left. Plain Java: the save, the reminders and the tests use it too.</p>
 */
public enum Language {
    EN("en", "English", "GB", false),
    FR("fr", "Français", "FR", false),
    AR("ar", "العربية", "SA", true),
    ES("es", "Español", "ES", false),
    PT("pt", "Português", "BR", false),
    DE("de", "Deutsch", "DE", false),
    ID("id", "Bahasa Indonesia", "ID", false),
    TR("tr", "Türkçe", "TR", false);

    /** ISO 639-1 code; also the name of the translation file ({@code i18n/<code>.json}). */
    public final String code;
    /** The language's name in itself, as the picker shows it. */
    public final String nativeName;
    /** ISO 3166 country of the flag drawn next to it. */
    public final String flag;
    /** Right-to-left script: text is shaped and reordered, layouts are mirrored. */
    public final boolean rtl;

    Language(String code, String nativeName, String flag, boolean rtl) {
        this.code = code;
        this.nativeName = nativeName;
        this.flag = flag;
        this.rtl = rtl;
    }

    /** @return the language with this code, or {@code null} for an unknown or empty one */
    public static Language fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (Language language : values()) {
            if (language.code.equalsIgnoreCase(code)) {
                return language;
            }
        }
        return null;
    }

    /**
     * The language to preselect for a device locale: its own language when the game has it,
     * English otherwise. "in" is the legacy Java code for Indonesian.
     */
    public static Language forLocale(Locale locale) {
        if (locale == null) {
            return EN;
        }
        String code = locale.getLanguage();
        if ("in".equals(code)) {
            code = "id";
        }
        Language match = fromCode(code);
        return match == null ? EN : match;
    }
}
