package com.ben.laama.sortpuzzle.logic;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The star road: every campaign star the player collects (1 to 3 per level, best result kept)
 * counts towards a row of milestones, and each milestone unlocks a skin that coins cannot buy.
 *
 * <p>Stars are a total, never spent: replaying a level for a better rating moves the player
 * along the road, and nothing they unlocked can be lost by playing. Pure data and arithmetic,
 * unit-tested; {@link Shop#isOwned} and the screens read it.</p>
 */
public final class StarRoad {

    /** One stop on the road. */
    public static final class Milestone {
        public final int stars;
        public final Cosmetic reward;

        Milestone(int stars, Cosmetic reward) {
            this.stars = stars;
            this.reward = reward;
        }
    }

    /**
     * Every {@link Cosmetic.Unlock#STARS} item of the catalogue, in increasing order of the
     * stars it needs ({@link Cosmetic#requirement()}); ties keep catalogue order.
     */
    public static final List<Milestone> MILESTONES = Collections.unmodifiableList(build());

    private StarRoad() {
    }

    private static List<Milestone> build() {
        List<Milestone> road = new ArrayList<>();
        for (Cosmetic item : Cosmetic.ALL) {
            if (item.unlock() == Cosmetic.Unlock.STARS) {
                road.add(new Milestone(item.requirement(), item));
            }
        }
        road.sort((a, b) -> Integer.compare(a.stars, b.stars));
        return road;
    }

    /** @return the stars needed for a star-road item, or {@code -1} if it is not on the road */
    public static int starsRequired(Cosmetic item) {
        return item.unlock() == Cosmetic.Unlock.STARS ? item.requirement() : -1;
    }

    public static boolean isUnlocked(Cosmetic item, int totalStars) {
        int required = starsRequired(item);
        return required >= 0 && totalStars >= required;
    }

    /** @return the next milestone still ahead, or {@code null} once the road is complete */
    public static Milestone next(int totalStars) {
        for (Milestone milestone : MILESTONES) {
            if (totalStars < milestone.stars) {
                return milestone;
            }
        }
        return null;
    }

    /** @return how many milestones are reached */
    public static int reachedCount(int totalStars) {
        int count = 0;
        for (Milestone milestone : MILESTONES) {
            if (totalStars >= milestone.stars) count++;
        }
        return count;
    }

    /**
     * Skins unlocked by going from {@code before} to {@code after} stars, in road order: what
     * the win screen announces. Empty when no milestone was crossed.
     */
    public static List<Cosmetic> unlockedBetween(int before, int after) {
        List<Cosmetic> items = new ArrayList<>();
        for (Milestone milestone : MILESTONES) {
            if (before < milestone.stars && after >= milestone.stars) {
                items.add(milestone.reward);
            }
        }
        return items;
    }

    /** Progress towards a milestone, 0..1, measured from the previous one. */
    public static float progressTo(Milestone target, int totalStars) {
        int from = 0;
        for (Milestone milestone : MILESTONES) {
            if (milestone == target) break;
            from = milestone.stars;
        }
        float span = target.stars - from;
        return span <= 0f ? 1f : Math.max(0f, Math.min(1f, (totalStars - from) / span));
    }
}
