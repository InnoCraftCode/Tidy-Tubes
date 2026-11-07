package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.i18n.T;

/**
 * The world map, as a sequence of biomes: every chapter of ten levels is a place with a name
 * and a look (one of the catalogue's themes), so progress is seen, not only counted. Biomes
 * cycle, with a roman numeral once the tour starts again, because the campaign has no end.
 *
 * <p>Pure data and arithmetic; screens read {@link #biomeFor} to name a chapter, tint its card
 * and (when the player has not chosen a theme of their own) dress the backdrop.</p>
 */
public final class World {

    public static final int CHAPTER_LEVELS = 10;

    /** A place on the map. */
    public static final class Biome {
        public final int chapter;        // 1-based
        public final String name;        // "Deep Sea II"
        public final String themeId;     // a Cosmetic THEME id
        public final int firstLevel;
        public final int lastLevel;

        Biome(int chapter, String name, String themeId, int firstLevel, int lastLevel) {
            this.chapter = chapter;
            this.name = name;
            this.themeId = themeId;
            this.firstLevel = firstLevel;
            this.lastLevel = lastLevel;
        }
    }

    private static final String[] NAMES = {
        T.k("Harbour"), T.k("Deep Sea"), T.k("Forest"), T.k("Lavender Fields"), T.k("Sakura Garden"),
        T.k("Ember Caves"), T.k("Golden Hour"), T.k("Midnight"), T.k("Aurora")
    };
    private static final String[] THEMES = {
        "theme.nocturne", "theme.ocean", "theme.forest", "theme.lavender", "theme.sakura",
        "theme.ember", "theme.golden", "theme.midnight", "theme.aurora"
    };
    private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

    private World() {
    }

    public static int chapterOf(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("levels are 1-based, got " + level);
        }
        return (level - 1) / CHAPTER_LEVELS + 1;
    }

    public static Biome biomeFor(int level) {
        return biomeOfChapter(chapterOf(level));
    }

    public static Biome biomeOfChapter(int chapter) {
        if (chapter < 1) {
            throw new IllegalArgumentException("chapters are 1-based, got " + chapter);
        }
        int index = (chapter - 1) % NAMES.length;
        int tour = (chapter - 1) / NAMES.length;
        String suffix = tour < ROMAN.length ? ROMAN[tour] : " " + (tour + 1);
        int first = (chapter - 1) * CHAPTER_LEVELS + 1;
        return new Biome(chapter, NAMES[index] + suffix, THEMES[index], first,
            first + CHAPTER_LEVELS - 1);
    }

    /** How far through its chapter a level is, 0..1 (level 1 of a chapter = 0.1). */
    public static float progressIn(Biome biome, int highestUnlocked) {
        int done = Math.max(0, Math.min(CHAPTER_LEVELS, highestUnlocked - biome.firstLevel));
        return done / (float) CHAPTER_LEVELS;
    }

    /** @return the number of biomes before names repeat */
    public static int biomeCount() {
        return NAMES.length;
    }
}
