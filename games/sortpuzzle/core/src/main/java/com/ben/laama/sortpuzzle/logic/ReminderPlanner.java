package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.i18n.T;
import com.ben.laama.sortpuzzle.notify.Reminder;
import com.ben.laama.sortpuzzle.notify.Reminders;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Decides which comeback reminders to schedule when the player leaves the game.
 *
 * <p>The rule (product decision, 2026-11-04): a first nudge {@link #FIRST_DELAY_MINUTES} after
 * the player leaves, then one every {@link #INTERVAL_HOURS}, each with a text that gives a
 * reason to come back. Nothing is posted during the quiet hours
 * ({@link #QUIET_START_HOUR}:00 to {@link #QUIET_END_HOUR}:00 local): a reminder that would
 * land there moves to {@link #QUIET_END_HOUR}:00 and the cadence continues from it. The plan
 * holds {@link Reminders#MAX_SCHEDULED} reminders at most; it is cancelled the moment the
 * player returns and rebuilt when they leave again, so it is always about where they are.</p>
 *
 * <p>Pure: the caller passes the local time and the save's fields, and gets back reminders
 * sorted by time, slots {@code 0..n-1}.</p>
 */
public final class ReminderPlanner {

    /** The first nudge, minutes after leaving. */
    public static final int FIRST_DELAY_MINUTES = 1;
    /** Then one every this many hours. */
    public static final int INTERVAL_HOURS = 4;
    /** No reminders from this local hour... */
    public static final int QUIET_START_HOUR = 23;
    /** ...until this one. */
    public static final int QUIET_END_HOUR = 8;

    private ReminderPlanner() {
    }

    /**
     * @param now                 current local time with its zone
     * @param lastDailyWinDate    ISO UTC date of the last daily win, "" if never
     * @param lastDailyStartDate  ISO UTC date of the last daily puzzle opened, "" if never
     * @param dailyWinStreak      consecutive daily wins ending at {@code lastDailyWinDate}
     * @param currentLevel        the campaign level the Play button resumes at
     * @return reminders sorted by time, slots 0..n-1
     */
    public static List<Reminder> plan(ZonedDateTime now, String lastDailyWinDate,
                                      String lastDailyStartDate, int dailyWinStreak,
                                      int currentLevel) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        String won = lastDailyWinDate == null ? "" : lastDailyWinDate;
        String started = lastDailyStartDate == null ? "" : lastDailyStartDate;
        int level = Math.max(1, currentLevel);

        List<Reminder> plan = new ArrayList<>();
        ZonedDateTime at = now.plusMinutes(FIRST_DELAY_MINUTES);
        for (int i = 0; i < Reminders.MAX_SCHEDULED; i++) {
            at = outsideQuietHours(at);
            LocalDate puzzleDay = at.withZoneSameInstant(ZoneOffset.UTC).toLocalDate();
            String puzzle = DailyChallenge.dateKey(puzzleDay);
            boolean dailyOpen = !puzzle.equals(won);
            boolean dailyStarted = dailyOpen && puzzle.equals(started);
            boolean streakAlive = dailyWinStreak > 0 && dailyOpen
                && won.equals(DailyChallenge.dateKey(puzzleDay.minusDays(1)));
            String[] text = text(i, level, dailyOpen, dailyStarted, streakAlive, dailyWinStreak);
            plan.add(new Reminder(i, at.toInstant().toEpochMilli(), text[0], text[1]));
            at = at.plusHours(INTERVAL_HOURS);
        }
        return plan;
    }

    /** Pre-1.1.0 signature, without the level: kept for callers that have none. */
    public static List<Reminder> plan(ZonedDateTime now, String lastDailyWinDate,
                                      String lastDailyStartDate, int dailyWinStreak) {
        return plan(now, lastDailyWinDate, lastDailyStartDate, dailyWinStreak, 1);
    }

    /** Moves a time that falls in the quiet hours to the next {@link #QUIET_END_HOUR}:00. */
    static ZonedDateTime outsideQuietHours(ZonedDateTime at) {
        int hour = at.getHour();
        boolean quiet = QUIET_START_HOUR > QUIET_END_HOUR
            ? hour >= QUIET_START_HOUR || hour < QUIET_END_HOUR
            : hour >= QUIET_START_HOUR && hour < QUIET_END_HOUR;
        if (!quiet) {
            return at;
        }
        ZonedDateTime morning = at.withHour(QUIET_END_HOUR).withMinute(0).withSecond(0).withNano(0);
        if (hour >= QUIET_START_HOUR) {
            morning = morning.plusDays(1);
        }
        return morning;
    }

    /**
     * @return {title, body} for the {@code index}-th reminder: the first one speaks to what was
     *         just left, the following ones rotate through reasons to come back
     */
    static String[] text(int index, int level, boolean dailyOpen, boolean dailyStarted,
                         boolean streakAlive, int streak) {
        // Notification text: logical order (Android shapes and lays out right-to-left itself).
        if (index == 0) {
            if (dailyStarted) {
                return new String[] {T.raw("Almost there!"),
                    T.raw("You started today's puzzle. A few more moves and it's solved.")};
            }
            return new String[] {T.rawf("Level {0} is waiting", LevelNumbers.format(level)),
                T.raw("Your tubes are right where you left them. One more?")};
        }
        if (streakAlive) {
            return new String[] {T.rawf("Keep your {0}-day streak", streak),
                T.rawf("Today's puzzle is ready. Solve it to make it {0} days in a row.", streak + 1)};
        }
        switch ((index - 1) % 5) {
            case 0:
                return dailyOpen
                    ? new String[] {T.raw("Today's puzzle is ready"),
                        T.raw("A fresh board, the same one for everyone today. Give it a minute.")}
                    : new String[] {T.raw("A calm minute?"),
                        T.rawf("Sort a few tubes, breathe out. Level {0} is waiting.", LevelNumbers.format(level))};
            case 1:
                return new String[] {T.raw("Can you beat your best?"),
                    T.rawf("Level {0} in fewer moves: the solver says it's possible.", LevelNumbers.format(level))};
            case 2:
                return new String[] {T.raw("Your tubes miss you"),
                    T.raw("Coins are waiting to be earned, and new styles to unlock in the shop.")};
            case 3:
                return dailyOpen
                    ? new String[] {T.raw("Still time for today's puzzle"),
                        T.raw("Win it before midnight (UTC) to keep your streak going.")}
                    : new String[] {T.raw("Three colours, then ten"),
                        T.rawf("Level {0} is the next step. Short, calm, satisfying.", LevelNumbers.format(level))};
            default:
                return new String[] {T.raw("One level before you go on"),
                    T.rawf("Sort. Breathe. Repeat. Level {0} takes about a minute.", LevelNumbers.format(level))};
        }
    }
}
