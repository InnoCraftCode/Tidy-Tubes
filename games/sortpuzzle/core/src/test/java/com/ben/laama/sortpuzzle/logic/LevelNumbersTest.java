package com.ben.laama.sortpuzzle.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LevelNumbers")
class LevelNumbersTest {

    @Test
    @DisplayName("numbers under a thousand are written in full")
    void smallNumbersStayExact() {
        assertEquals("1", LevelNumbers.format(1));
        assertEquals("999", LevelNumbers.format(999));
    }

    @Test
    @DisplayName("round thousands drop the decimal")
    void roundThousandsAreShort() {
        assertEquals("1k", LevelNumbers.format(1_000));
        assertEquals("5k", LevelNumbers.format(5_000));
        assertEquals("1k", LevelNumbers.format(1_050));
    }

    @Test
    @DisplayName("thousands keep one truncated decimal")
    void thousandsKeepOneDecimal() {
        assertEquals("1.2k", LevelNumbers.format(1_234));
        assertEquals("1.2k", LevelNumbers.format(1_299));
        assertEquals("999.9k", LevelNumbers.format(999_971));
        assertEquals("999.9k", LevelNumbers.format(999_999));
    }

    @Test
    @DisplayName("the last level reads one million")
    void millionIsOneM() {
        assertEquals("1M", LevelNumbers.format(1_000_000));
    }
}
