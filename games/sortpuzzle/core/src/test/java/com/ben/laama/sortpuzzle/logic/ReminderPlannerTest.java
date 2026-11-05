package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.notify.Reminder;
import com.ben.laama.sortpuzzle.notify.Reminders;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReminderPlanner")
class ReminderPlannerTest {

    /** Morocco-like UTC+1. */
    private static final ZoneOffset PLUS_ONE = ZoneOffset.ofHours(1);

    private static ZonedDateTime at(int day, int hour, int minute, ZoneOffset zone) {
        return ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, zone);
    }

    private static ZonedDateTime local(Reminder r, ZoneOffset zone) {
        return Instant.ofEpochMilli(r.atMillis).atZone(zone);
    }

    @Test
    @DisplayName("first nudge one minute after leaving, then every four hours, slots in order")
    void cadence() {
        ZonedDateTime left = at(3, 10, 0, PLUS_ONE);
        List<Reminder> plan = ReminderPlanner.plan(left, "", "", 0, 12);
        assertEquals(Reminders.MAX_SCHEDULED, plan.size());
        assertEquals(left.plusMinutes(1).toInstant().toEpochMilli(), plan.get(0).atMillis);
        assertEquals("Level 12 is waiting", plan.get(0).title);
        ZonedDateTime second = local(plan.get(1), PLUS_ONE);
        assertEquals(14, second.getHour());
        assertEquals(1, second.getMinute());
        for (int i = 0; i < plan.size(); i++) {
            assertEquals(i, plan.get(i).slot);
            if (i > 0) {
                assertTrue(plan.get(i).atMillis > plan.get(i - 1).atMillis);
            }
        }
    }

    @Test
    @DisplayName("nothing lands in the quiet hours: a 23:00 slot moves to 08:00 and the cadence restarts there")
    void quietHours() {
        List<Reminder> plan = ReminderPlanner.plan(at(3, 18, 30, PLUS_ONE), "", "", 0, 3);
        // 18:31, 22:31, then 02:31 -> 08:00 next day, 12:00, 16:00, 20:00, 00:00 -> 08:00 ...
        int[] expectedHours = {18, 22, 8, 12, 16, 20, 8, 12, 16, 20, 8, 12};
        for (int i = 0; i < plan.size(); i++) {
            ZonedDateTime when = local(plan.get(i), PLUS_ONE);
            assertEquals(expectedHours[i], when.getHour(), "reminder " + i);
            boolean quiet = when.getHour() >= ReminderPlanner.QUIET_START_HOUR
                || when.getHour() < ReminderPlanner.QUIET_END_HOUR;
            assertFalse(quiet, "reminder " + i + " at " + when);
        }
        assertEquals(4, local(plan.get(2), PLUS_ONE).getDayOfMonth());
    }

    @Test
    @DisplayName("leaving at night: even the first nudge waits for the morning")
    void leavingAtNight() {
        List<Reminder> plan = ReminderPlanner.plan(at(3, 23, 30, PLUS_ONE), "", "", 0, 1);
        ZonedDateTime first = local(plan.get(0), PLUS_ONE);
        assertEquals(4, first.getDayOfMonth());
        assertEquals(ReminderPlanner.QUIET_END_HOUR, first.getHour());
        assertEquals(0, first.getMinute());
    }

    @Test
    @DisplayName("a daily puzzle started but not finished gets the 'almost there' first nudge")
    void startedDaily() {
        List<Reminder> plan = ReminderPlanner.plan(at(3, 10, 0, PLUS_ONE), "", "2026-11-03", 0, 5);
        assertEquals("Almost there!", plan.get(0).title);
    }

    @Test
    @DisplayName("an alive streak is the reason given, with the right count")
    void streakText() {
        List<Reminder> plan = ReminderPlanner.plan(at(3, 10, 0, PLUS_ONE), "2026-11-02", "", 4, 5);
        assertEquals("Keep your 4-day streak", plan.get(1).title);
        assertTrue(plan.get(1).body.contains("5 days in a row"));
    }

    @Test
    @DisplayName("follow-ups rotate through different reasons; the level number is in them")
    void textsVary() {
        List<Reminder> plan = ReminderPlanner.plan(at(3, 8, 0, PLUS_ONE), "2026-11-03", "", 0, 27);
        Set<String> titles = new HashSet<>();
        for (int i = 1; i <= 5; i++) {
            titles.add(plan.get(i).title);
        }
        assertTrue(titles.size() >= 4, titles.toString());
        boolean mentionsLevel = false;
        for (Reminder r : plan) {
            if (r.body.contains("Level 27") || r.title.contains("Level 27")) mentionsLevel = true;
        }
        assertTrue(mentionsLevel);
    }

    @Test
    @DisplayName("null time is rejected; a missing level counts as level 1")
    void guards() {
        assertThrows(IllegalArgumentException.class,
            () -> ReminderPlanner.plan(null, "", "", 0, 1));
        List<Reminder> plan = ReminderPlanner.plan(at(3, 10, 0, PLUS_ONE), "", "", 0);
        assertEquals("Level 1 is waiting", plan.get(0).title);
    }
}
