package com.ben.laama.sortpuzzle.notify;

import java.util.List;

/**
 * Local (on-device, no server) notifications, behind an interface like {@code AdProvider}.
 *
 * <p>The Android implementation posts through {@code AlarmManager} and a broadcast receiver;
 * desktop and tests get {@link NoOpReminders}. What to say and when is decided by the pure
 * {@code ReminderPlanner}; this interface only delivers.</p>
 */
public interface Reminders {

    /** Upper bound on scheduled reminders; the planner never asks for more. */
    /** Enough for about two days of the 4-hour cadence; the plan is rebuilt on every leave. */
    int MAX_SCHEDULED = 12;

    /** Called on the render thread with the system's answer. */
    interface PermissionCallback {
        void onResult(boolean granted);
    }

    /** @return {@code false} where notifications do not exist (desktop): hide the UI */
    boolean isSupported();

    /** @return whether the system currently lets the app post notifications */
    boolean hasPermission();

    /**
     * Shows the system permission prompt (Android 13+). Answers {@code true} immediately where
     * no runtime permission exists, {@code false} where the prompt can no longer be shown.
     */
    void requestPermission(PermissionCallback callback);

    /** Opens the system notification settings for this app (after a permanent denial). */
    void openSettings();

    /** Replaces everything previously scheduled with {@code reminders}. */
    void schedule(List<Reminder> reminders);

    /** Cancels every scheduled reminder and clears the ones already shown. */
    void cancelAll();

    /** @return {@code true} once after the player opened the game by tapping a reminder */
    boolean consumeOpenDailyRequest();

    /** @return {@code true} on builds that can post a test reminder (debug builds only) */
    default boolean supportsTest() {
        return false;
    }

    /**
     * Posts one test reminder after {@code seconds}, through the same alarm, receiver and
     * channel as the real ones, so the whole path can be checked on a device in a minute.
     */
    default void scheduleTest(int seconds) {
    }
}
