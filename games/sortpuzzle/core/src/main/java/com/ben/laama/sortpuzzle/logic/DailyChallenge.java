package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.GameConfig;

import java.time.LocalDate;
import java.time.ZoneOffset;

/** Stable, offline daily puzzle selection shared by every installation. */
public final class DailyChallenge {

    private static final long SEED_SALT = 0xD1B54A32D192ED03L;
    private static final int DIFFICULTY_CYCLE = 100;

    private DailyChallenge() {
    }

    public static LocalDate todayUtc() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    public static String dateKey(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        return date.toString();
    }

    /**
     * All players get the same difficulty profile for a given UTC day, always from the hard
     * band: levels {@code DAILY_FIRST_LEVEL} to {@code DAILY_FIRST_LEVEL + 99} (8-10 colours).
     */
    public static int levelFor(LocalDate date) {
        return GameConfig.DAILY_FIRST_LEVEL
            + Math.floorMod(requireDate(date).toEpochDay(), DIFFICULTY_CYCLE);
    }

    /** A day-specific seed keeps the daily board distinct from its campaign counterpart. */
    public static long seedFor(LocalDate date) {
        long value = requireDate(date).toEpochDay() + SEED_SALT;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static LocalDate requireDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        return date;
    }
}
