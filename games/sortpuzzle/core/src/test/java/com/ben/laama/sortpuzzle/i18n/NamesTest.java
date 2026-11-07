package com.ben.laama.sortpuzzle.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NamesTest {

    @AfterEach
    void reset() {
        T.install(Language.EN, null);
    }

    @Test
    void englishGeneratedNameHasNoTranslatorNote() {
        T.install(Language.EN, null);

        String name = Names.name("Azure Smoke II");

        assertEquals("Azure Smoke II", name);
    }

    @Test
    void translatedWordOrderPutsTheNounFirst() {
        Map<String, String> fr = new HashMap<>();
        fr.put(Names.ORDER, "{1} {0}");
        fr.put("Ruby", "rubis");
        fr.put("Harbor", "Port");
        T.install(Language.FR, fr);

        String name = Names.name("Ruby Harbor");

        assertEquals("Port rubis", name);
    }
}
