package com.ben.laama.sortpuzzle.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArabicTextTest {

    @Test
    @DisplayName("letters take their initial, medial and final forms")
    void contextualForms() {
        // مرحبا: meem initial, ra final, hah initial (ra does not join forward), beh medial, alef final
        assertEquals("ﻣﺮﺣﺒﺎ", ArabicText.shape("مرحبا"));
    }

    @Test
    @DisplayName("lam followed by alef becomes one ligature, joined to the letter before")
    void lamAlef() {
        // سلام: seen initial, lam-alef final ligature, meem isolated (alef never joins forward)
        assertEquals("ﺳﻼﻡ", ArabicText.shape("سلام"));
        // لا alone: isolated ligature
        assertEquals("ﻻ", ArabicText.shape("لا"));
    }

    @Test
    @DisplayName("diacritics are dropped and do not break the joining around them")
    void harakatAreDroppedWithoutBreakingJoins() {
        // رتّب (ra, teh, shadda, beh): same forms as رتب, no shadda glyph in the output
        assertEquals(ArabicText.shape("رتب"), ArabicText.shape("رتّب"));
    }

    @Test
    @DisplayName("a lone letter and a hamza stay isolated")
    void isolated() {
        assertEquals("ﺏ", ArabicText.shape("ب"));
        assertEquals("ﺀ", ArabicText.shape("ء"));
    }

    @Test
    @DisplayName("visual order reverses Arabic but keeps numbers left to right")
    void visualOrder() {
        // "ا 12": in a right-to-left line the number ends up on the left, digits unreversed
        assertEquals("12 ﺍ", ArabicText.visual("ا 12"));
        // سلام reads right to left: drawn meem first
        assertEquals("ﻡﻼﺳ", ArabicText.visual("سلام"));
    }

    @Test
    @DisplayName("each line of a multi-line text is reordered on its own, lines keep their order")
    void lines() {
        assertEquals("ﺍ\nﺏ", ArabicText.visual("ا\nب"));
    }

    @Test
    @DisplayName("text without Arabic is returned untouched")
    void latin() {
        String text = "Level 12 (hard)";
        assertSame(text, ArabicText.visual(text));
        assertFalse(ArabicText.hasArabic(text));
        assertTrue(ArabicText.hasArabic("ا"));
    }

    @Test
    @DisplayName("brackets are mirrored inside a right-to-left run")
    void brackets() {
        // (ب) logical -> drawn as (ب) after mirroring both brackets in the reversed run
        assertEquals("(ﺏ)", ArabicText.visual("(ب)"));
    }
}
