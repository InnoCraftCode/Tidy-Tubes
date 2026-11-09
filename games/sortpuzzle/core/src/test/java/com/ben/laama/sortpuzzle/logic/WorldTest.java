package com.ben.laama.sortpuzzle.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("World map")
class WorldTest {

    @Test
    @DisplayName("ten levels per chapter, biomes in order, themes that exist in the catalogue")
    void chaptersAndBiomes() {
        assertEquals(1, World.chapterOf(1));
        assertEquals(1, World.chapterOf(10));
        assertEquals(2, World.chapterOf(11));
        World.Biome harbour = World.biomeFor(7);
        assertEquals("Harbour", harbour.name);
        assertEquals(1, harbour.firstLevel);
        assertEquals(10, harbour.lastLevel);
        World.Biome sea = World.biomeFor(11);
        assertEquals("Deep Sea", sea.name);
        assertEquals("theme.ocean", sea.themeId);
        for (int chapter = 1; chapter <= 40; chapter++) {
            World.Biome biome = World.biomeOfChapter(chapter);
            assertNotNull(Cosmetic.byId(biome.themeId), biome.themeId);
            assertEquals(Cosmetic.Category.THEME, Cosmetic.byId(biome.themeId).category());
        }
    }

    @Test
    @DisplayName("after a full tour the names repeat without a number")
    void namesRepeatWithoutNumber() {
        int count = World.biomeCount();
        assertEquals("Harbour", World.biomeOfChapter(count + 1).name);
        assertEquals("Deep Sea", World.biomeOfChapter(2 * count + 2).name);
        assertEquals("Harbour", World.biomeOfChapter(100_000).name); // the last chapter
    }

    @Test
    @DisplayName("chapter progress follows the highest unlocked level")
    void progress() {
        World.Biome second = World.biomeOfChapter(2); // levels 11-20
        assertEquals(0f, World.progressIn(second, 11), 1e-6);
        assertEquals(0.5f, World.progressIn(second, 16), 1e-6);
        assertEquals(1f, World.progressIn(second, 21), 1e-6);
        assertEquals(1f, World.progressIn(second, 99), 1e-6);
        assertEquals(0f, World.progressIn(second, 3), 1e-6);
    }

    @Test
    @DisplayName("levels and chapters are 1-based")
    void guards() {
        assertThrows(IllegalArgumentException.class, () -> World.chapterOf(0));
        assertThrows(IllegalArgumentException.class, () -> World.biomeOfChapter(0));
        assertTrue(World.biomeCount() >= 8);
    }
}
