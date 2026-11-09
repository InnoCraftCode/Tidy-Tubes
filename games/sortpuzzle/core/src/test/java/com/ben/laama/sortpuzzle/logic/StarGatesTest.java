package com.ben.laama.sortpuzzle.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ben.laama.sortpuzzle.save.SaveData;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StarGates")
class StarGatesTest {

    @Test
    @DisplayName("only every tenth chapter has a gate")
    void everyTenthChapterIsGated() {
        assertFalse(StarGates.isGated(1));
        assertFalse(StarGates.isGated(9));
        assertTrue(StarGates.isGated(10));
        assertFalse(StarGates.isGated(11));
        assertTrue(StarGates.isGated(20));
        assertTrue(StarGates.isGated(100_000));
    }

    @Test
    @DisplayName("a gate asks two stars per level before the chapter")
    void requiredStarsAreTwoPerPreviousLevel() {
        assertEquals(0, StarGates.requiredStars(9));
        assertEquals(180, StarGates.requiredStars(10)); // levels 1-90
        assertEquals(380, StarGates.requiredStars(20)); // levels 1-190
        assertEquals(580, StarGates.requiredStars(30));
    }

    @Test
    @DisplayName("the gate opens exactly at the required star count")
    void gateOpensAtThreshold() {
        assertFalse(StarGates.isOpen(10, 179));
        assertTrue(StarGates.isOpen(10, 180));
        assertTrue(StarGates.isOpen(11, 0)); // no gate
    }

    @Test
    @DisplayName("reaching a gated chapter is not enough without the stars")
    void gatedChapterNeedsStars() {
        SaveData save = new SaveData();
        save.highestUnlocked = 91; // first level of chapter 10
        save.bestStars = new int[90];
        java.util.Arrays.fill(save.bestStars, 2);
        save.bestStars[0] = 1; // 179 stars, one short

        assertFalse(StarGates.canPlay(save, 91));
        assertTrue(StarGates.canPlay(save, 90)); // chapter 9 has no gate
    }

    @Test
    @DisplayName("one more star opens the gated chapter")
    void gatedChapterOpensWithEnoughStars() {
        SaveData save = new SaveData();
        save.highestUnlocked = 91;
        save.bestStars = new int[90];
        java.util.Arrays.fill(save.bestStars, 2); // 180 stars

        assertTrue(StarGates.canPlay(save, 91));
        assertFalse(StarGates.canPlay(save, 92)); // not reached yet
    }

    @Test
    @DisplayName("a player already past a gate keeps access to its levels")
    void gateAlreadyCrossedStaysOpen() {
        SaveData save = new SaveData();
        save.highestUnlocked = 150; // went past chapter 10 before gates existed, 0 stars

        assertTrue(StarGates.canPlay(save, 95));
        assertTrue(StarGates.canPlay(save, 150));
    }

    @Test
    @DisplayName("the next target is the first closed gate ahead, or nothing once it is open")
    void nextTargetIsTheNextClosedGate() {
        SaveData save = new SaveData();
        save.highestUnlocked = 45; // chapter 5: the next gate is chapter 10
        assertEquals(180, StarGates.nextTarget(save));

        save.highestUnlocked = 91; // standing at chapter 10's gate
        save.bestStars = new int[90];
        java.util.Arrays.fill(save.bestStars, 2); // 180 stars: open
        assertEquals(0, StarGates.nextTarget(save));
    }

    @Test
    @DisplayName("daily wins add no campaign stars")
    void dailyStarsDoNotCount() {
        SaveData save = new SaveData();

        save.recordDailyWin("2026-11-09");

        assertEquals(0, save.totalStars());
    }
}
