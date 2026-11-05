package com.ben.laama.sortpuzzle.save;

import com.ben.laama.sortpuzzle.logic.Cosmetic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Saved cosmetics")
class SaveDataCosmeticsTest {

    @Test
    @DisplayName("a pre-1.1.0 palette index is carried over to the equipped palette id")
    void legacyPaletteMigrates() {
        SaveData save = new SaveData();
        save.ballPalette = 2;
        save.equippedBallPalette = null; // what an old blob leaves behind
        save.ownedCosmetics = null;
        assertTrue(save.migrateCosmetics());
        assertEquals(Cosmetic.BALL_NEON.id(), save.equippedBallPalette);
        assertEquals(0, save.ownedCosmetics.length);
        assertFalse(save.migrateCosmetics(), "idempotent");

        SaveData fresh = new SaveData();
        assertFalse(fresh.migrateCosmetics());
        assertEquals("", fresh.equippedBallPalette);
    }

    @Test
    @DisplayName("the legacy index stays in step with the equipped palette id")
    void legacyIndexFollowsEquippedId() {
        SaveData save = new SaveData();
        save.setEquippedCosmetic(Cosmetic.Category.BALL_PALETTE, Cosmetic.BALL_PASTEL.id());
        assertEquals(1, save.ballPalette);
        save.setEquippedCosmetic(Cosmetic.Category.BALL_PALETTE, Cosmetic.BALL_CANDY.id());
        assertEquals(0, save.ballPalette); // unknown to old builds: they show the classic one
        save.setEquippedCosmetic(Cosmetic.Category.BALL_PALETTE, Cosmetic.BALL_NEON.id());
        assertEquals(2, save.ballPalette);
    }

    @Test
    @DisplayName("ownership and announcements are sets: no duplicates, reset clears them")
    void ownershipIsASet() {
        SaveData save = new SaveData();
        save.addOwnedCosmetic("tube.neon");
        save.addOwnedCosmetic("tube.neon");
        save.addOwnedCosmetic("");
        save.addOwnedCosmetic(null);
        assertEquals(1, save.ownedCosmetics.length);
        assertTrue(save.ownsCosmetic("tube.neon"));
        assertFalse(save.ownsCosmetic("tube.gold"));

        save.markShopNotified("ball.candy");
        save.markShopNotified("ball.candy");
        assertEquals(1, save.shopNotified.length);
        assertTrue(save.isShopNotified("ball.candy"));

        for (Cosmetic.Category category : Cosmetic.Category.values()) {
            save.setEquippedCosmetic(category, "x." + category.name());
            assertEquals("x." + category.name(), save.equippedCosmetic(category));
        }
        save.reset();
        assertEquals(0, save.ownedCosmetics.length);
        assertEquals(0, save.shopNotified.length);
        for (Cosmetic.Category category : Cosmetic.Category.values()) {
            assertEquals("", save.equippedCosmetic(category));
        }
    }
}
