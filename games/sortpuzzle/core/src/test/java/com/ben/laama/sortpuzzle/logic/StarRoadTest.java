package com.ben.laama.sortpuzzle.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ben.laama.sortpuzzle.save.SaveData;

import java.util.List;

import org.junit.jupiter.api.Test;

class StarRoadTest {

    @Test
    void everyStarItemIsOnTheRoadOnceAndInOrder() {
        int previous = 0;
        for (StarRoad.Milestone milestone : StarRoad.MILESTONES) {
            assertTrue(milestone.stars >= previous, "milestones must not go down");
            assertEquals(Cosmetic.Unlock.STARS, milestone.reward.unlock());
            previous = milestone.stars;
        }
        int onRoad = 0;
        for (Cosmetic item : Cosmetic.ALL) {
            if (item.unlock() == Cosmetic.Unlock.STARS) onRoad++;
        }
        assertEquals(onRoad, StarRoad.MILESTONES.size());
    }

    @Test
    void handMadeMilestonesKeepTheirStars() {
        assertEquals(30, StarRoad.starsRequired(Cosmetic.BALL_NEON));
        assertEquals(60, StarRoad.starsRequired(Cosmetic.TUBE_STARLIGHT));
        assertEquals(100, StarRoad.starsRequired(Cosmetic.THEME_STARFALL));
        assertEquals(-1, StarRoad.starsRequired(Cosmetic.BALL_CANDY));
        assertFalse(StarRoad.isUnlocked(Cosmetic.BALL_NEON, 29));
        assertTrue(StarRoad.isUnlocked(Cosmetic.BALL_NEON, 30));
    }

    @Test
    void firstMilestonesComeEarlyAndInterleaveCategories() {
        // 30 (Spectrum), then generated 45, 60 (Starlight), 65, 85, 100 (Starfall) ...
        assertEquals(30, StarRoad.MILESTONES.get(0).stars);
        assertEquals(45, StarRoad.MILESTONES.get(1).stars);
        assertEquals(60, StarRoad.MILESTONES.get(2).stars);
        assertTrue(StarRoad.MILESTONES.size() >= 80, "about a quarter of 400 skins");
    }

    @Test
    void crossingMilestonesAnnouncesEachSkinOnce() {
        assertTrue(StarRoad.unlockedBetween(10, 29).isEmpty());
        assertEquals(List.of(Cosmetic.BALL_NEON), StarRoad.unlockedBetween(29, 31));
        List<Cosmetic> two = StarRoad.unlockedBetween(29, 45);
        assertEquals(2, two.size());
        assertEquals(Cosmetic.BALL_NEON, two.get(0));
        assertTrue(StarRoad.unlockedBetween(60, 60).isEmpty()); // replay without a better rating
    }

    @Test
    void nextMilestoneAndProgress() {
        StarRoad.Milestone next = StarRoad.next(50);
        assertEquals(60, next.stars);
        assertEquals(Cosmetic.TUBE_STARLIGHT, next.reward);
        assertEquals(1f / 3f, StarRoad.progressTo(next, 50), 0.001f); // 45 -> 60
        assertEquals(2, StarRoad.reachedCount(50));
        assertNull(StarRoad.next(1_000_000));
    }

    @Test
    void shopOwnsStarSkinsFromTheSaveTotalAndNeverSellsThem() {
        SaveData save = new SaveData();
        for (int level = 1; level <= 20; level++) {
            save.recordWin(level, 10, 10); // par = 3 stars each
        }
        assertEquals(60, save.totalStars());
        assertTrue(Shop.isOwned(save, Cosmetic.TUBE_STARLIGHT));
        assertFalse(Shop.isOwned(save, Cosmetic.THEME_STARFALL));
        assertEquals("60 / 100 stars", Shop.starProgress(save, Cosmetic.THEME_STARFALL));
        assertEquals("", Shop.starProgress(save, Cosmetic.TUBE_STARLIGHT));
        assertEquals(Shop.PurchaseResult.NOT_FOR_SALE, Shop.buy(save, Cosmetic.THEME_STARFALL));
        assertTrue(Shop.equip(save, Cosmetic.TUBE_STARLIGHT));
        assertFalse(Shop.equip(save, Cosmetic.THEME_STARFALL));
    }

    @Test
    void chapterRewardIsOwnedOnceTheChapterIsCleared() {
        Cosmetic reward = null;
        for (Cosmetic item : Cosmetic.ALL) {
            if (item.unlock() == Cosmetic.Unlock.CHAPTER && item.requirement() == 2) reward = item;
        }
        SaveData save = new SaveData();
        for (int level = 1; level <= 19; level++) {
            save.recordWin(level, 10, 10);
        }
        assertFalse(Shop.isOwned(save, reward), "chapter 2 is levels 11-20");
        List<Cosmetic> before = Shop.ownedProgressionItems(save);
        save.recordWin(20, 10, 10);
        assertTrue(Shop.isOwned(save, reward));
        List<Cosmetic> after = Shop.ownedProgressionItems(save);
        assertTrue(after.contains(reward) && !before.contains(reward));
        assertTrue(Shop.unlockHint(reward).startsWith("Clear chapter 2"));
    }
}
