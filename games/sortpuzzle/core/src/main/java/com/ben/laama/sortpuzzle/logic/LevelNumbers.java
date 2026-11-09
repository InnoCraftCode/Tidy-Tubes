package com.ben.laama.sortpuzzle.logic;

/**
 * Short display form of a level number: 999 stays 999, 1000 is "1k", 1234 is "1.2k",
 * 999971 is "999.9k" and 1000000 is "1M". The decimal is truncated, never rounded up, so a
 * level never reads as a bigger milestone than it is (999950 is "999.9k", not "1000k").
 *
 * <p>Display only: saves, analytics and level generation keep the exact integer.</p>
 */
public final class LevelNumbers {

    private LevelNumbers() {
    }

    public static String format(int level) {
        if (level < 1_000) {
            return Integer.toString(level);
        }
        if (level < 1_000_000) {
            return compact(level / 100, "k");      // tenths of a thousand
        }
        return compact(level / 100_000, "M");      // tenths of a million
    }

    private static String compact(int tenths, String suffix) {
        int whole = tenths / 10;
        int decimal = tenths % 10;
        return decimal == 0 ? whole + suffix : whole + "." + decimal + suffix;
    }
}
