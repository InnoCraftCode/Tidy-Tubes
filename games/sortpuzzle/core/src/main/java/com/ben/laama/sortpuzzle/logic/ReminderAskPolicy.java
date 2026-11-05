package com.ben.laama.sortpuzzle.logic;

/**
 * When to show the in-game "Turn on reminders?" card (the soft-ask) before the system prompt.
 *
 * <p>A cold system prompt on first launch is usually denied for good; explaining first, at a
 * calm moment and only to players who came back, gets far more "Allow". Rules (workspace spec
 * 13-notifications): from the {@link #MIN_SESSIONS}th session, never while reminders are on or
 * after the system said no, at most {@link #MAX_ASKS} times, {@link #RE_ASK_DAYS} days apart.</p>
 */
public final class ReminderAskPolicy {

    public static final int MIN_SESSIONS = 3;
    public static final int MAX_ASKS = 3;
    public static final int RE_ASK_DAYS = 3;

    private ReminderAskPolicy() {
    }

    /**
     * @param sessions        launches so far, this one included
     * @param asks            soft-asks already answered "Not now"
     * @param nextAskEpochDay earliest local epoch day for the next ask (0 = any time)
     * @param todayEpochDay   today's local epoch day
     * @param remindersOn     the player already turned reminders on
     * @param systemDenied    the system prompt was answered "Don't allow"
     */
    public static boolean shouldAsk(int sessions, int asks, long nextAskEpochDay,
                                    long todayEpochDay, boolean remindersOn,
                                    boolean systemDenied) {
        return !remindersOn && !systemDenied && sessions >= MIN_SESSIONS && asks < MAX_ASKS
            && todayEpochDay >= nextAskEpochDay;
    }

    /** @return the epoch day of the next allowed ask after a "Not now" today */
    public static long nextAskAfterDecline(long todayEpochDay) {
        return todayEpochDay + RE_ASK_DAYS;
    }
}
