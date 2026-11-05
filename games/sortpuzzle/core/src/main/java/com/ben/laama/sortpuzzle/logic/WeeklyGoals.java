package com.ben.laama.sortpuzzle.logic;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Fixed, offline weekly goals and their date key. */
public final class WeeklyGoals {

    public static final int GOAL_COUNT = 3;
    public static final int CLEAR_LEVELS = 0;
    public static final int FINISH_UNDER_PAR = 1;
    public static final int PLAY_MOVES = 2;

    private static final int[] TARGETS = {3, 1, 20};
    private static final String[] TITLES = {
        "Clear 3 campaign levels",
        "Finish a level at par",
        "Make 20 campaign moves"
    };

    private WeeklyGoals() {
    }

    public static String weekKey(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
    }

    public static int target(int goal) {
        checkGoal(goal);
        return TARGETS[goal];
    }

    public static String title(int goal) {
        checkGoal(goal);
        return TITLES[goal];
    }

    public static int rewardCoins() {
        return 30;
    }

    public static int progress(int goal, int campaignWins, int parFinishes, int moves) {
        checkGoal(goal);
        switch (goal) {
            case CLEAR_LEVELS:
                return Math.max(0, campaignWins);
            case FINISH_UNDER_PAR:
                return Math.max(0, parFinishes);
            case PLAY_MOVES:
                return Math.max(0, moves);
            default:
                throw new IllegalStateException("unknown goal: " + goal);
        }
    }

    private static void checkGoal(int goal) {
        if (goal < 0 || goal >= GOAL_COUNT) {
            throw new IllegalArgumentException("unknown weekly goal: " + goal);
        }
    }
}
