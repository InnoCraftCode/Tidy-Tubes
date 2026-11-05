package com.ben.laama.sortpuzzle.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the catalogue data: ids, defaults, prices, and that every look stays readable. */
@DisplayName("Cosmetic catalogue")
class CosmeticCatalogueTest {

    private static final String TEXT = "F4F6FF";
    private static final double MIN_TEXT_CONTRAST = 7.0;
    private static final double MIN_BALL_DISTANCE = 50.0;
    private static final double MIN_BALL_ON_BACKDROP = 60.0;

    @Test
    @DisplayName("ids are unique and resolve back to the item")
    void idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (Cosmetic item : Cosmetic.ALL) {
            assertTrue(ids.add(item.id()), "duplicate id " + item.id());
            assertEquals(item, Cosmetic.byId(item.id()));
            assertTrue(item.id().startsWith(prefix(item.category())), item.id());
        }
        assertNull(Cosmetic.byId("theme.removed"));
        assertNull(Cosmetic.byId(null));
    }

    @Test
    @DisplayName("exactly one free item per category, listed first")
    void oneDefaultPerCategory() {
        for (Cosmetic.Category category : Cosmetic.Category.values()) {
            List<Cosmetic> items = Cosmetic.of(category);
            int free = 0;
            for (Cosmetic item : items) {
                if (item.isDefault()) free++;
            }
            assertEquals(1, free, category.name());
            assertTrue(items.get(0).isDefault(), category + " must start with its free item");
            assertEquals(items.get(0), Cosmetic.defaultFor(category));
        }
    }

    @Test
    @DisplayName("prices sit on the tiers; progression items cost nothing")
    void priceTiers() {
        Set<Integer> tiers = new HashSet<>(List.of(0, 250, 300, 400, 500, 600, 750, 900, 1200, 1500));
        for (Cosmetic item : Cosmetic.ALL) {
            int price = item.price();
            assertTrue(tiers.contains(price), item.id() + " costs " + price);
            if (item.unlock() != Cosmetic.Unlock.NONE) {
                assertEquals(0, price, item.id() + " is earned, not sold");
            }
        }
    }

    @Test
    @DisplayName("100 items per category, mostly for coins, a quarter on the star road, some chapter rewards")
    void hundredPerCategoryWithAMixOfUnlocks() {
        for (Cosmetic.Category category : Cosmetic.Category.values()) {
            List<Cosmetic> items = Cosmetic.of(category);
            assertEquals(Cosmetic.PER_CATEGORY, items.size(), category.name());
            int coins = 0;
            int stars = 0;
            int chapters = 0;
            for (Cosmetic item : items) {
                if (item.isForSale()) coins++;
                if (item.unlock() == Cosmetic.Unlock.STARS) stars++;
                if (item.unlock() == Cosmetic.Unlock.CHAPTER) {
                    chapters++;
                    assertTrue(item.requirement() >= 1, item.id());
                }
            }
            assertTrue(coins >= 55 && coins <= 65, category + " coin items " + coins);
            assertTrue(stars >= 20 && stars <= 30, category + " star items " + stars);
            assertTrue(chapters >= 10 && chapters <= 18, category + " chapter items " + chapters);
        }
    }

    @Test
    @DisplayName("chapter rewards: one per chapter, no gaps, from chapter 1")
    void chapterRewardsAreOnePerChapter() {
        Set<Integer> chapters = new HashSet<>();
        for (Cosmetic item : Cosmetic.ALL) {
            if (item.unlock() == Cosmetic.Unlock.CHAPTER) {
                assertTrue(chapters.add(item.requirement()), "two rewards for chapter " + item.requirement());
            }
        }
        for (int chapter = 1; chapter <= chapters.size(); chapter++) {
            assertTrue(chapters.contains(chapter), "no reward for chapter " + chapter);
        }
    }

    @Test
    @DisplayName("every finish has a known lighting base; generated ones carry a visible pattern")
    void finishesHaveABaseAndAPattern() {
        Set<String> bases = Set.of("finish.glossy", "finish.matte", "finish.glass", "finish.gem",
            "finish.jelly", "finish.chrome");
        for (Cosmetic finish : Cosmetic.of(Cosmetic.Category.BALL_FINISH)) {
            assertTrue(bases.contains(finish.finishBase()), finish.id());
            if (finish.pattern() != Cosmetic.Pattern.NONE) {
                assertTrue(finish.patternCount() >= 1, finish.id());
                assertTrue(finish.patternStrength() > 0f && finish.patternStrength() <= 0.3f, finish.id());
            }
        }
    }

    @Test
    @DisplayName("names are unique within a category")
    void namesAreUniquePerCategory() {
        for (Cosmetic.Category category : Cosmetic.Category.values()) {
            Set<String> names = new HashSet<>();
            for (Cosmetic item : Cosmetic.of(category)) {
                assertTrue(names.add(item.displayName()), category + " repeats " + item.displayName());
            }
        }
    }

    @Test
    @DisplayName("every palette has ten colours that stay apart from each other")
    void palettesAreDistinguishable() {
        for (Cosmetic palette : Cosmetic.of(Cosmetic.Category.BALL_PALETTE)) {
            assertEquals(10, palette.hexCount(), palette.id());
            for (int i = 0; i < 10; i++) {
                for (int j = i + 1; j < 10; j++) {
                    double distance = Contrast.distance(palette.hex(i), palette.hex(j));
                    assertTrue(distance >= MIN_BALL_DISTANCE,
                        palette.id() + " colours " + i + " and " + j + " too close: " + distance);
                }
            }
        }
    }

    @Test
    @DisplayName("text stays readable and balls stay visible on every theme")
    void themesKeepContrast() {
        for (Cosmetic theme : Cosmetic.of(Cosmetic.Category.THEME)) {
            assertEquals(4, theme.hexCount(), theme.id());
            for (int i = 0; i < 2; i++) {
                double ratio = Contrast.ratio(TEXT, theme.hex(i));
                assertTrue(ratio >= MIN_TEXT_CONTRAST,
                    theme.id() + " backdrop " + i + " text contrast " + ratio);
            }
            for (Cosmetic palette : Cosmetic.of(Cosmetic.Category.BALL_PALETTE)) {
                for (int i = 0; i < palette.hexCount(); i++) {
                    double distance = Contrast.distance(palette.hex(i), theme.hex(0));
                    assertTrue(distance >= MIN_BALL_ON_BACKDROP,
                        palette.id() + " colour " + i + " vanishes on " + theme.id() + ": " + distance);
                }
            }
        }
    }

    @Test
    @DisplayName("tube styles carry a body tint, a rim colour and a stroke width")
    void tubeStylesAreComplete() {
        for (Cosmetic tube : Cosmetic.of(Cosmetic.Category.TUBE)) {
            assertEquals(2, tube.hexCount(), tube.id());
            assertEquals(8, tube.hex(0).length(), tube.id() + " body needs RGBA");
            assertEquals(8, tube.hex(1).length(), tube.id() + " rim needs RGBA");
            assertTrue(tube.rimThickness() >= 2f && tube.rimThickness() <= 5f, tube.id());
        }
        assertNotNull(Cosmetic.legacyBallPalette(7)); // out of range maps to the classic palette
        assertEquals(Cosmetic.BALL_CLASSIC, Cosmetic.legacyBallPalette(7));
        assertEquals(Cosmetic.BALL_PASTEL, Cosmetic.legacyBallPalette(1));
        assertEquals(Cosmetic.BALL_NEON, Cosmetic.legacyBallPalette(2));
    }

    private static String prefix(Cosmetic.Category category) {
        switch (category) {
            case BALL_PALETTE:
                return "ball.";
            case BALL_FINISH:
                return "finish.";
            case TUBE:
                return "tube.";
            default:
                return "theme.";
        }
    }
}
