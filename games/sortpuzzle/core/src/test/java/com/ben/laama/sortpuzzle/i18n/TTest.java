package com.ben.laama.sortpuzzle.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TTest {

    @AfterEach
    void reset() {
        T.install(Language.EN, null);
    }

    @Test
    @DisplayName("English is the key: no table, the text comes back as written")
    void englishFallback() {
        assertEquals("Play", T.r("Play"));
        assertEquals("Level 7", T.f("Level {0}", 7));
    }

    @Test
    @DisplayName("a translated pattern fills its placeholders, apostrophes stay plain text")
    void translatedPattern() {
        Map<String, String> fr = new HashMap<>();
        fr.put("Level {0}", "Niveau {0}");
        fr.put("{0} of {1}", "{1} : l'étape {0}");
        T.install(Language.FR, fr);
        assertEquals("Niveau 3", T.f("Level {0}", 3));
        assertEquals("B : l'étape A", T.f("{0} of {1}", "A", "B"));
        assertEquals("Missing", T.r("Missing"));
        assertTrue(T.has("Level {0}"));
        assertFalse(T.has("Missing"));
    }

    @Test
    @DisplayName("right-to-left text is shaped for drawing; raw stays logical for the platform")
    void rtl() {
        Map<String, String> ar = new HashMap<>();
        ar.put("Hi", "سلام");
        T.install(Language.AR, ar);
        assertTrue(T.rtl());
        assertEquals("سلام", T.raw("Hi"));
        assertEquals("ﻡﻼﺳ", T.r("Hi"));
    }

    @Test
    @DisplayName("device locale preselects its language, Indonesian legacy code included")
    void locale() {
        assertEquals(Language.FR, Language.forLocale(Locale.FRANCE));
        assertEquals(Language.ID, Language.forLocale(new Locale("in", "ID")));
        assertEquals(Language.EN, Language.forLocale(Locale.JAPAN));
        assertEquals(Language.AR, Language.fromCode("ar"));
    }
}
