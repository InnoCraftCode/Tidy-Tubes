package com.ben.laama.sortpuzzle.notify;

import java.util.List;

/** Desktop and tests: no notifications exist, so the settings row and the soft-ask are hidden. */
public final class NoOpReminders implements Reminders {

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public boolean hasPermission() {
        return false;
    }

    @Override
    public void requestPermission(PermissionCallback callback) {
        callback.onResult(false);
    }

    @Override
    public void openSettings() {
    }

    @Override
    public void schedule(List<Reminder> reminders) {
    }

    @Override
    public void cancelAll() {
    }

    @Override
    public boolean consumeOpenDailyRequest() {
        return false;
    }
}
