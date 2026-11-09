package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.save.SaveData;

/**
 * Star gates: chapters 10, 20, 30... open only when the player holds enough campaign stars,
 * two per level before the chapter on average (chapter 10 = levels 91-100 needs 180 stars).
 * Only campaign stars count ({@link SaveData#totalStars()}); daily wins are stored apart.
 * Stars only ever go up, so a gate that opened stays open and nothing needs saving.
 */
public final class StarGates {

    private StarGates() {
    }

    public static boolean isGated(int chapter) {
        return chapter >= GameConfig.STAR_GATE_EVERY_CHAPTERS
            && chapter % GameConfig.STAR_GATE_EVERY_CHAPTERS == 0;
    }

    /** @return the campaign stars needed to open {@code chapter}, or 0 when it has no gate */
    public static int requiredStars(int chapter) {
        if (!isGated(chapter)) {
            return 0;
        }
        int levelsBefore = (chapter - 1) * World.CHAPTER_LEVELS;
        return GameConfig.STAR_GATE_STARS_PER_LEVEL * levelsBefore;
    }

    public static boolean isOpen(int chapter, int campaignStars) {
        return campaignStars >= requiredStars(chapter);
    }

    /**
     * Whether {@code chapter} is open for this player. A chapter the player already went past
     * stays open, so players who crossed a gate before gates existed are never locked out of
     * levels they had reached.
     */
    public static boolean isOpenFor(SaveData save, int chapter, int campaignStars) {
        boolean alreadyPast = save.highestUnlocked > World.biomeOfChapter(chapter).lastLevel;
        return alreadyPast || isOpen(chapter, campaignStars);
    }

    /**
     * The stars the player is working towards: the requirement of the first gate at or after
     * their current chapter that is still closed for them, or {@code 0} when none is in reach
     * (the gate after it only matters once this one is open).
     */
    public static int nextTarget(SaveData save) {
        int stars = save.totalStars();
        int chapter = World.chapterOf(save.highestUnlocked);
        int every = GameConfig.STAR_GATE_EVERY_CHAPTERS;
        int gate = ((chapter + every - 1) / every) * every; // this chapter or the next gated one
        if (gate > World.chapterOf(GameConfig.MAX_LEVEL)) {
            return 0;
        }
        return isOpenFor(save, gate, stars) ? 0 : requiredStars(gate);
    }

    /** A level can be played when it is unlocked by progress and its chapter's gate is open. */
    public static boolean canPlay(SaveData save, int level) {
        return save.isUnlocked(level) && isOpenFor(save, World.chapterOf(level), save.totalStars());
    }
}
