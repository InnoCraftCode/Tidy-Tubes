package com.ben.laama.sortpuzzle.notify;

/** One local notification to post at a given instant. Immutable. */
public final class Reminder {

    /** Stable slot 0..{@link Reminders#MAX_SCHEDULED}-1; scheduling a slot replaces it. */
    public final int slot;
    /** Epoch milliseconds, UTC. */
    public final long atMillis;
    public final String title;
    public final String body;

    public Reminder(int slot, long atMillis, String title, String body) {
        if (slot < 0 || slot >= Reminders.MAX_SCHEDULED) {
            throw new IllegalArgumentException("slot out of range: " + slot);
        }
        if (title == null || title.isEmpty() || body == null || body.isEmpty()) {
            throw new IllegalArgumentException("a reminder needs a title and a body");
        }
        this.slot = slot;
        this.atMillis = atMillis;
        this.title = title;
        this.body = body;
    }

    @Override
    public String toString() {
        return "Reminder{" + slot + " @" + atMillis + " " + title + "}";
    }
}
