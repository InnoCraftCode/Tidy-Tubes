package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.save.SaveData;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Daily rewards calendar")
class DailyRewardsTest {

    private static final LocalDate D1 = LocalDate.of(2026, 11, 4);

    @Test
    @DisplayName("first ever claim is day 1; consecutive days advance; a gap restarts")
    void dayProgression() {
        assertEquals(1, DailyRewards.dayFor("", 0, D1));
        assertEquals(2, DailyRewards.dayFor("2026-11-04", 1, D1.plusDays(1)));
        assertEquals(0, DailyRewards.dayFor("2026-11-04", 1, D1));          // already claimed today
        assertEquals(1, DailyRewards.dayFor("2026-11-04", 3, D1.plusDays(2))); // missed a day
        assertEquals(1, DailyRewards.dayFor("2026-11-04", 7, D1.plusDays(1))); // week wraps
        assertEquals(1, DailyRewards.dayFor("garbage", 3, D1));
    }

    @Test
    @DisplayName("rewards are defined for seven days and the labels read right")
    void rewardsAndLabels() {
        assertEquals(7, DailyRewards.COINS.length);
        assertEquals(7, DailyRewards.BOOSTERS.length);
        assertEquals("+25", DailyRewards.label(1));
        assertEquals("+40 & 1 tube", DailyRewards.label(4));
        assertEquals("+150 & 1 tube", DailyRewards.label(7));
        assertTrue(DailyRewards.coinsFor(7) > DailyRewards.coinsFor(1));
        assertThrows(IllegalArgumentException.class, () -> DailyRewards.coinsFor(8));
    }

    @Test
    @DisplayName("the save pays once per day and keeps the streak over a week")
    void saveClaims() {
        SaveData save = new SaveData();
        int coins = 0;
        int boosters = 0;
        for (int day = 1; day <= 7; day++) {
            LocalDate date = D1.plusDays(day - 1);
            assertTrue(DailyRewards.canClaim(save.dailyRewardDate, save.dailyRewardDay, date));
            assertEquals(day, save.claimDailyReward(date));
            assertEquals(0, save.claimDailyReward(date), "second claim the same day pays nothing");
            coins += DailyRewards.coinsFor(day);
            boosters += DailyRewards.boostersFor(day);
            assertEquals(coins, save.coins);
            assertEquals(boosters, save.boosters);
        }
        assertEquals(1, save.claimDailyReward(D1.plusDays(7)), "day 8 starts a new week");
        assertEquals(1, save.claimDailyReward(D1.plusDays(10)), "a gap restarts at day 1");
        assertFalse(DailyRewards.canClaim(save.dailyRewardDate, save.dailyRewardDay, D1.plusDays(10)));
        assertEquals(1, DailyRewards.displayDay(save.dailyRewardDate, save.dailyRewardDay, D1.plusDays(10)));
        save.reset();
        assertEquals("", save.dailyRewardDate);
        assertEquals(0, save.dailyRewardDay);
    }
}
