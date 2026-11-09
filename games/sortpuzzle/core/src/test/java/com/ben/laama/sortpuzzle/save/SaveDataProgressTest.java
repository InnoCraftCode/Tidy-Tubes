package com.ben.laama.sortpuzzle.save;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.logic.Achievement;
import com.ben.laama.sortpuzzle.logic.WeeklyGoals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Saved player rewards")
class SaveDataProgressTest {

    @Test
    @DisplayName("winning the last level unlocks nothing past one million")
    void lastLevelHasNoSuccessor() {
        SaveData save = new SaveData();
        save.highestUnlocked = GameConfig.MAX_LEVEL;
        save.currentLevel = GameConfig.MAX_LEVEL;

        save.recordWin(GameConfig.MAX_LEVEL, 30, 25);

        assertEquals(1_000_000, save.highestUnlocked);
        assertEquals(1_000_000, save.currentLevel);
        assertTrue(save.isCompleted(GameConfig.MAX_LEVEL));
    }

    @Test
    @DisplayName("daily wins pay once per date and only consecutive dates grow the streak")
    void dailyRewardAndStreak() {
        SaveData save = new SaveData();
        assertEquals(GameConfig.DAILY_CHALLENGE_COINS, save.recordDailyWin("2026-10-30"));
        assertEquals(1, save.dailyWinStreak);
        assertEquals(0, save.recordDailyWin("2026-10-30"));
        assertEquals(0, save.recordDailyWin("2026-10-29"));
        assertEquals(GameConfig.DAILY_CHALLENGE_COINS, save.recordDailyWin("2026-10-31"));
        assertEquals(2, save.dailyWinStreak);
        assertFalse(save.dailyPaletteUnlocked);
        assertEquals(GameConfig.DAILY_CHALLENGE_COINS, save.recordDailyWin("2026-11-01"));
        assertTrue(save.dailyPaletteUnlocked);
        assertEquals(GameConfig.DAILY_PALETTE_STREAK, save.dailyWinStreak);
        assertEquals(GameConfig.DAILY_CHALLENGE_COINS * 3, save.coins);
    }

    @Test
    @DisplayName("rewarded daily bonus pays once per date and only advances to newer dates")
    void dailyRewardedBonusPaysOncePerDate() {
        SaveData save = new SaveData();
        assertTrue(save.claimDailyRewardedBonus("2026-11-01"));
        assertEquals(GameConfig.DAILY_REWARDED_BONUS_COINS, save.coins);
        assertTrue(save.hasClaimedDailyRewardedBonus("2026-11-01"));
        assertFalse(save.claimDailyRewardedBonus("2026-11-01"));
        assertFalse(save.claimDailyRewardedBonus("2026-10-31"));
        assertTrue(save.claimDailyRewardedBonus("2026-11-02"));
        assertEquals(GameConfig.DAILY_REWARDED_BONUS_COINS * 2, save.coins);
    }

    @Test
    @DisplayName("music selection starts unset and accepts only one of the ten generated tracks")
    void musicTrackSelectionIsValidated() {
        SaveData save = new SaveData();
        assertEquals(-1, save.musicTrack);
        save.setMusicTrack(9);
        assertEquals(9, save.musicTrack);
        assertThrows(IllegalArgumentException.class, () -> save.setMusicTrack(-1));
        assertThrows(IllegalArgumentException.class, () -> save.setMusicTrack(10));
    }

    @Test
    @DisplayName("rewarded extra tubes grant once then wait 90 seconds before another reward")
    void rewardedTubeCooldown() {
        SaveData save = new SaveData();
        long rewardedAt = 1_000_000L;
        assertTrue(save.claimRewardedTube(rewardedAt));
        assertEquals(1, save.boosters);
        assertEquals(GameConfig.REWARDED_TUBE_COOLDOWN_MS,
            save.rewardedTubeCooldownRemaining(rewardedAt));
        assertEquals(GameConfig.REWARDED_TUBE_COOLDOWN_MS,
            save.rewardedTubeCooldownRemaining(rewardedAt - 1L));
        assertFalse(save.claimRewardedTube(rewardedAt + 89_999L));
        assertEquals(1, save.boosters);
        assertEquals(1L, save.rewardedTubeCooldownRemaining(rewardedAt + 89_999L));
        long nextRewardAt = rewardedAt + GameConfig.REWARDED_TUBE_COOLDOWN_MS;
        assertTrue(save.claimRewardedTube(nextRewardAt));
        assertEquals(2, save.boosters);
        assertEquals(GameConfig.REWARDED_TUBE_COOLDOWN_MS,
            save.rewardedTubeCooldownRemaining(nextRewardAt));
        assertFalse(save.claimRewardedTube(nextRewardAt));
    }

    @Test
    @DisplayName("missing a day restarts the consecutive streak")
    void missedDayRestartsStreak() {
        SaveData save = new SaveData();
        save.recordDailyWin("2026-10-30");
        save.recordDailyWin("2026-11-01");
        assertEquals(1, save.dailyWinStreak);
    }

    @Test
    @DisplayName("palette selection is gated until its daily streak reward is earned")
    void paletteUnlockIsEnforced() {
        SaveData save = new SaveData();
        assertThrows(IllegalArgumentException.class, () -> save.setBallPalette(1));
        save.recordDailyWin("2026-10-30");
        save.recordDailyWin("2026-10-31");
        save.recordDailyWin("2026-11-01");
        save.setBallPalette(1);
        assertEquals(1, save.ballPalette);
    }

    @Test
    @DisplayName("campaign stars keep the best result when a level is replayed")
    void campaignStarsAreBestPerLevel() {
        SaveData save = new SaveData();
        save.recordWin(1, 18, 12);
        assertEquals(1, save.bestStarsFor(1));
        save.recordWin(1, 15, 12);
        assertEquals(2, save.bestStarsFor(1));
        save.recordWin(1, 12, 12);
        assertEquals(3, save.bestStarsFor(1));
        save.recordWin(1, 20, 12);
        assertEquals(3, save.bestStarsFor(1));
        assertEquals(3, save.totalStars());
    }

    @Test
    @DisplayName("weekly goals pay once and reset at the next UTC Monday")
    void weeklyGoalsPayOnceAndReset() {
        SaveData save = new SaveData();
        save.beginWeeklyGoals(java.time.LocalDate.of(2026, 11, 2));
        assertEquals(GameConfig.WEEKLY_GOAL_REWARD, save.recordWeeklyCampaignWin(
            java.time.LocalDate.of(2026, 11, 2), 8, true));
        assertEquals(1, save.weeklyProgress(WeeklyGoals.CLEAR_LEVELS));
        assertEquals(1, save.weeklyProgress(WeeklyGoals.FINISH_UNDER_PAR));
        assertEquals(8, save.weeklyProgress(WeeklyGoals.PLAY_MOVES));

        assertEquals(GameConfig.WEEKLY_GOAL_REWARD, save.recordWeeklyCampaignWin(
            java.time.LocalDate.of(2026, 11, 3), 12, false));
        assertEquals(GameConfig.WEEKLY_GOAL_REWARD, save.recordWeeklyCampaignWin(
            java.time.LocalDate.of(2026, 11, 4), 4, false));
        assertEquals((GameConfig.WEEKLY_GOAL_REWARD * 3), save.coins);
        assertEquals(0, save.claimCompletedWeeklyGoals());

        save.beginWeeklyGoals(java.time.LocalDate.of(2026, 11, 9));
        assertEquals(0, save.weeklyProgress(WeeklyGoals.CLEAR_LEVELS));
        assertEquals(0, save.weeklyClaimedMask);
    }

    @Test
    @DisplayName("replayed campaign moves count without farming unique-level goals")
    void weeklyMovesIncludeReplaysButUniqueClearGoalDoesNot() {
        SaveData save = new SaveData();
        java.time.LocalDate monday = java.time.LocalDate.of(2026, 11, 2);
        save.recordWeeklyCampaignResult(monday, 1, true, false);
        assertEquals(GameConfig.WEEKLY_GOAL_REWARD,
            save.recordWeeklyCampaignResult(monday, 19, false, false));
        assertEquals(1, save.weeklyProgress(WeeklyGoals.CLEAR_LEVELS));
        assertEquals(20, save.weeklyProgress(WeeklyGoals.PLAY_MOVES));
    }

    @Test
    @DisplayName("achievements unlock and pay only once")
    void achievementsPayOnce() {
        SaveData save = new SaveData();
        int firstClearReward = save.recordWin(1, 20, 10);
        assertTrue(firstClearReward >= Achievement.FIRST_CLEAR.reward());
        assertTrue(save.isAchievementUnlocked(Achievement.FIRST_CLEAR));
        int coinsAfterFirstClear = save.coins;
        save.recordWin(1, 21, 10);
        assertEquals(coinsAfterFirstClear + 8, save.coins);
    }

    @Test
    @DisplayName("the spectrum palette unlocks from stars without a daily streak")
    void starPaletteUnlocks() {
        SaveData save = new SaveData();
        for (int level = 1; level <= 10; level++) {
            save.recordWin(level, 1, 1);
        }
        assertTrue(save.totalStars() >= GameConfig.STAR_PALETTE_UNLOCK);
        assertTrue(save.isCosmeticUnlocked(2));
        save.setBallPalette(2);
        assertEquals(2, save.ballPalette);
    }
}
