package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.i18n.T;
import java.time.LocalDate;

/**
 * The seven-day login calendar: one claim per local day, the reward grows while the days are
 * consecutive, a missed day starts the week again. Day 4 and day 7 add a booster, so the
 * calendar hands out both currencies the game has.
 *
 * <p>Pure: the save holds the last claim date and the day index, and asks here what today is
 * worth. No clock inside - the caller passes the date, so tests pick any day they like.</p>
 */
public final class DailyRewards {

    public static final int DAYS = 7;
    /** Coins per day, index 0 = day 1. */
    public static final int[] COINS = {25, 35, 50, 40, 75, 100, 150};
    /** Boosters (extra tubes) per day, index 0 = day 1. */
    public static final int[] BOOSTERS = {0, 0, 0, 1, 0, 0, 1};

    private DailyRewards() {
    }

    /**
     * @param lastClaimDate ISO date of the last claim, "" if never
     * @param lastDay       the day index (1..7) claimed on that date, 0 if never
     * @param today         the local date now
     * @return the day index (1..7) that a claim today would be, or 0 when today is already claimed
     */
    public static int dayFor(String lastClaimDate, int lastDay, LocalDate today) {
        if (lastClaimDate == null || lastClaimDate.isEmpty() || lastDay < 1 || lastDay > DAYS) {
            return 1;
        }
        LocalDate last;
        try {
            last = LocalDate.parse(lastClaimDate);
        } catch (RuntimeException invalid) {
            return 1;
        }
        if (last.equals(today)) {
            return 0;
        }
        if (last.plusDays(1).equals(today)) {
            return lastDay % DAYS + 1;
        }
        return 1;
    }

    public static boolean canClaim(String lastClaimDate, int lastDay, LocalDate today) {
        return dayFor(lastClaimDate, lastDay, today) > 0;
    }

    public static int coinsFor(int day) {
        return COINS[index(day)];
    }

    public static int boostersFor(int day) {
        return BOOSTERS[index(day)];
    }

    /** "+50" / "+1 tube" / "+150 & tube", for the calendar dots and the claim button. */
    public static String label(int day) {
        int coins = coinsFor(day);
        int boosters = boostersFor(day);
        // Logical (unshaped) text in the interface language: callers pass it to T.f or T.show.
        if (coins > 0 && boosters > 0) {
            return boosters > 1 ? T.rawf("+{0} & {1} tubes", coins, boosters)
                : T.rawf("+{0} & {1} tube", coins, boosters);
        }
        if (boosters > 0) {
            return boosters > 1 ? T.rawf("+{0} tubes", boosters) : T.rawf("+{0} tube", boosters);
        }
        return "+" + coins;
    }

    /**
     * The day shown as "today" on the calendar: the claimable day, or the day already claimed
     * when today is done.
     */
    public static int displayDay(String lastClaimDate, int lastDay, LocalDate today) {
        int day = dayFor(lastClaimDate, lastDay, today);
        return day == 0 ? lastDay : day;
    }

    private static int index(int day) {
        if (day < 1 || day > DAYS) {
            throw new IllegalArgumentException("day must be 1.." + DAYS + ", got " + day);
        }
        return day - 1;
    }
}
